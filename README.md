# star-site-backend

star-site 后端：Spring Cloud Alibaba 微服务架构。

## 技术栈

| 组件 | 版本 |
|---|---|
| JDK | 21 |
| Spring Boot | 3.2.4 |
| Spring Cloud | 2023.0.1 |
| Spring Cloud Alibaba | 2023.0.1.2（Nacos 注册/配置中心，server v2.3.2） |
| MyBatis-Plus | 3.5.12（spring-boot3-starter） |
| 认证 | jjwt 0.12.7 + BCrypt（spring-security-crypto） |
| 消息 | RocketMQ 5.x（rocketmq-spring-boot-starter 2.3.4，事务消息 + 延迟消息） |

## 模块分层

模块命名两级区分：**`star-site-platform-*` 是横向平台服务**（账号/文件/提醒/网关，其他项目可直接复用），其余为 star-site 业务域。Java 包名暂统一在 `com.xp.starsite` 下，跨项目复用时再统一重构（如 `com.xp.platform`）。

| 层 | 模块 | 端口 / Nacos 名 | 说明 |
|---|---|---|---|
| 公共库 | `star-site-common` | — | 统一响应 `R<T>`、异常、JWT、MQ 封装（EventPublisher / 幂等消费基类 / 事件信封） |
| 契约 | `star-site-api` | — | Feign 契约（FamilyApi + FamilyOwnershipGuard）+ 领域事件（Reminder 事件族） |
| 平台 | `star-site-platform-gateway` | 8080 / star-site-platform-gateway | WebFlux 网关：路由 + JWT 白名单校验 + CORS |
| 平台 | `star-site-platform-member-service` | 8081 / star-site-platform-member | 注册/登录、个人资料、改密 |
| 平台 | `star-site-platform-file-service` | 8083 / star-site-platform-file | 上传/预览，本地磁盘/OSS 双实现 |
| 平台 | `star-site-platform-reminder-service` | 8086 / star-site-platform-reminder | 提醒中心：事件消费落规则、每日扫描、站内通知/铃铛 |
| 业务 | `star-site-family-service` | 8082 / star-site-family | 家庭、家庭成员档案；`/internal/family/**` 归属校验 |
| 业务 | `star-site-health-service` | 8084 / star-site-health | 体检报告、指标、趋势、搜索 |
| 业务 | `star-site-insurance-service` | 8085 / star-site-insurance | 保单档案、保障地图、缴费/到期提醒事件（事务消息） |
| 业务 | `star-site-archive-service` | 8087 / star-site-archive | 家庭档案：证件夹、房产车辆台账、估值历史 |

## 架构规约（新服务必须遵守）

1. **域隔离**：垂直域之间不读对方表、不跨域 JOIN；同步取数走 `star-site-api` 的 Feign 接口（`/internal/**` 前缀，网关不路由），异步联动走 MQ 领域事件。
2. **归属校验强制**：凡按 id / familyMemberId 取数，先经 `FamilyOwnershipGuard` 校验归属，失败返回 403；列表类接口不传成员 id 时也收口在自己家庭范围内（fail-closed）。
3. **事件驱动**：跨域通知/联动一律发事件（`EventPublisher.publishTransactional` 保落库与发消息原子），不搞服务链式同步调用；消费端以 `(consumerGroup, eventId)` 落 `event_consume` 表幂等。
4. **MQ 显式装配**：common 的 MQ 封装不含 @Component；业务服务 `@Import(StarSiteMqConfig.class)` + 配置 `rocketmq.name-server` 才启用，网关/未用 MQ 的服务不受影响。
5. **表规约**：新域表带 `family_id + owner_member_id` 双冗余、`created_at/updated_at`、逻辑删除 `deleted`；引用他域实体存 id + 展示快照字段；发事务消息的表主键用应用侧雪花 id（ASSIGN_ID），insert 前即拿到 bizId。

## API

统一响应体：`{code, message, data}`，`code=0` 成功；403 = 无权限（越权拦截）。

| 服务 | 方法 | 路径 | 说明 |
|---|---|---|---|
| member | POST | /api/auth/register | 注册 `{username, password, nickname?}` |
| member | POST | /api/auth/login | 登录 → `{token, expiresIn, member}` |
| member | GET/PUT | /api/member/me | 资料（Bearer token） |
| member | PUT | /api/member/me/password | 改密 `{oldPassword, newPassword}` |
| family | POST/GET | /api/family、/api/family/mine | 家庭创建/查询 |
| family | POST/GET/PUT/DELETE | /api/family/members[/{id}] | 家庭成员 CRUD |
| family | GET | /internal/family/ownership、/internal/family/members | 服务间归属校验（不经网关） |
| file | POST | /api/file/upload | 上传（≤20MB，pdf/jpg/png/webp） |
| file | GET | /api/file/{id}/view | 预览（仅上传者本人） |
| health | POST/GET | /api/health/reports | 报告创建/分页 |
| health | GET | /api/health/reports/{id} | 详情（含指标） |
| health | POST | /api/health/reports/{id}/indicators | 指标批量录入 |
| health | DELETE | /api/health/reports/{id} | 删除 |
| health | GET | /api/health/trends、/api/health/search | 趋势/搜索 |
| insurance | POST/GET | /api/insurance/policies | 保单创建/分页（筛选：被保人/险种/状态） |
| insurance | GET/PUT/DELETE | /api/insurance/policies/{id} | 详情/更新/删除 |
| insurance | GET | /api/insurance/coverage-map | 保障地图（成员 × 险种矩阵） |
| reminder | GET | /api/reminder/upcoming | 未来 N 天要办的事 |
| reminder | GET | /api/reminder/notifications[...]、PUT .../read | 通知列表/未读数/已读 |
| reminder | PUT | /api/reminder/occurrences/{id}/done、/dismiss | 完成/忽略提醒 |
| reminder | POST | /internal/reminder/scan | 手动触发扫描（内网） |
| archive | POST/GET | /api/archive/documents | 证件创建/分页 |
| archive | GET/PUT/DELETE | /api/archive/documents/{id} | 详情/更新/删除 |
| archive | POST/GET | /api/archive/assets | 资产创建/分页 |
| archive | GET | /api/archive/assets/summary | 资产总览（总估值/按类型） |
| archive | GET | /api/archive/assets/{id}/valuations | 估值历史 |
| finance | POST/GET | /api/finance/accounts | 账户建户（初始余额自动留痕）/列表 |
| finance | GET/PUT/DELETE | /api/finance/accounts/{id} | 详情/编辑/注销（余额须为 0） |
| finance | POST | /api/finance/transactions | 记账（收入/支出/转账/调整），流水与余额同事务 |
| finance | GET | /api/finance/transactions | 流水分页（账户/方向/年月筛选） |
| finance | DELETE | /api/finance/transactions/{id} | 删除流水并回滚余额（转账整组回滚） |
| * | GET | /actuator/health | 探活 |

## 本地开发

```bash
# 1. 起基础设施（MySQL 8 / Redis 7 / Nacos / RocketMQ）
cd docker && docker compose up -d

# 2. 编译 + 单测
mvn clean verify

# 3. 跑单个服务（示例：会员服务）
mvn -pl star-site-platform-member-service spring-boot:run

# 3'. 或全栈容器化（不可变基础设施）：构建 8 个服务镜像并连同基础设施一起拉起
docker compose --profile app up -d --build
# （配置经环境变量覆盖指向容器网络内的 Nacos/MySQL/RocketMQ，JWT_SECRET 注入）

# 4. 验证
curl http://127.0.0.1:8081/actuator/health
# Nacos 控制台 http://127.0.0.1:8848/nacos
```

默认连 `127.0.0.1:3306`（root/root123，库 star_site，建表脚本在
`docker/mysql-init/01~09.sql`，容器首次启动自动执行；已有 volume 的老库需手动执行 05-mq.sql、06-insurance.sql、07-reminder.sql、08-archive.sql、09-finance.sql）。

RocketMQ 说明：

- namesrv 9876 / broker 10911，broker.conf 在 `docker/rocketmq/`（`brokerIP1=127.0.0.1`，宿主机客户端可达的关键）
- 国内环境先经加速源拉取并 retag `apache/rocketmq:5.3.1`（compose 为 `pull_policy: never`；多源轮询脚本 `docker/pull-rocketmq.sh`）
- 业务服务接入 MQ：pom 引 `rocketmq-spring-boot-starter`，yml 配 `rocketmq.name-server: 127.0.0.1:9876` + `rocketmq.producer.group`，启动类/配置类 `@Import(StarSiteMqConfig.class)`

## 生产部署（ECS）注意

- 所有容器加 `mem_limit`，MySQL buffer pool 降 64M，Nacos XMX 192m，JVM 显式 `-Xmx`
- ECS 有公网 IP：Nacos 必须开鉴权并改默认密钥；`jwt.secret` 用环境变量注入
- 不部署 Sentinel Dashboard / Prometheus 等重型组件（机器 1.8G 内存）；RocketMQ 默认不启（reminder 降级为纯 @Scheduled 扫描）
