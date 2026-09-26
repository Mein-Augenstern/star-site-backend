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

## 模块

- `star-site-common`：统一响应 `R<T>`、`BizException` + 全局异常处理、`JwtUtil`、常量
- `star-site-member-service`：会员微服务（端口 8081，注册到 Nacos 为 `star-site-member`）

## API

统一响应体：`{code, message, data}`，`code=0` 成功。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/auth/register | 注册 `{username, password, nickname?}` |
| POST | /api/auth/login | 登录 → `{token, expiresIn, member}` |
| GET | /api/member/me | 查个人资料（Bearer token） |
| PUT | /api/member/me | 编辑资料 `{nickname, avatar, email, phone, gender, bio}` |
| PUT | /api/member/me/password | 改密 `{oldPassword, newPassword}` |
| GET | /actuator/health | 探活 |

## 本地开发

```bash
# 1. 起基础设施（MySQL 8 / Redis 7 / Nacos）
cd docker && docker compose up -d

# 2. 编译
mvn clean install -DskipTests

# 3. 跑会员服务
mvn -pl star-site-member-service spring-boot:run

# 4. 验证
curl http://127.0.0.1:8081/actuator/health
# Nacos 控制台 http://127.0.0.1:8848/nacos
```

默认连 `127.0.0.1:3306`（root/root123，库 star_site，建表脚本在
`docker/mysql-init/01-schema.sql`，容器首次启动自动执行）。

## 生产部署（ECS）注意

- 所有容器加 `mem_limit`，MySQL buffer pool 降 64M，Nacos XMX 192m，JVM 显式 `-Xmx`
- ECS 有公网 IP：Nacos 必须开鉴权并改默认密钥；`jwt.secret` 用环境变量注入
- 不部署 Sentinel Dashboard / Prometheus 等重型组件（机器 1.8G 内存）
