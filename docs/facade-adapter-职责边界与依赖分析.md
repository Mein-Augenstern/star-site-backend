# facade / adapter(web) 模块职责边界与依赖分析

> 2026-11-13：针对「facade 与 adapter 合并为一个模块（都是统一以 HTTP 对外提供服务）」
> 提议的全量核查结论。分析产出当日未改代码；**§7.1 统一 `*-web` 命名已于同日实施**
> （5 个 `*-adapter` 改名完成，全仓编译与 reminder-web 测试通过）。

## 1. 结论摘要

facade **不是「对外提供 HTTP 服务」的模块**，它是**给其他服务用的客户端契约**
（Feign 接口 + DTO + MQ 事件）；真正提供 HTTP 的是 adapter/web 里的 `/internal/**` 控制器。
两者是同一份契约的两端，但消费者和依赖足迹完全不同，直接合并有两个硬障碍：

| # | 障碍 | 性质 |
|---|---|---|
| 1 | **Maven 环形依赖**：6/7 有 facade 的服务满足 `common-biz → facade` 且 `adapter → common-biz`，合并即 `X → common-biz → X` | Maven 硬错误，构建直接失败 |
| 2 | **跨服务 classpath 污染**：所有 start 均 `scanBasePackages = "com.xp.starsite"` 全包扫描，合并后消费方传递拉入提供方全链实现（控制器/service/mapper 被扫进消费方进程） | 运行期事故：幽灵 bean、缺表、路径冲突 |

真正的不一致在另一处：HTTP 对外模块**命名分裂**（5×`*-adapter` vs 5×`*-web`），
这是可以零风险统一的（见 §7）。

## 2. 职责边界

### facade —— 消费方视角的轻量契约

- **内容**：`@FeignClient` 接口（`com.xp.starsite.api.client.*Api`）、跨服务 DTO
  （`api.dto.*`）、MQ 事件载荷（`api.event.*`）、可选的 Guard 工具类（`FamilyOwnershipGuard`，
  故意不加 `@Component`，由消费方在配置类 `@Bean` 引入）
- **依赖**（刻意保持最轻，7 个 facade 全部一致）：`star-site-common` +
  `spring-cloud-openfeign-core`（provided）+ lombok（provided）
- **交付形态**：仅 reactor 内部 jar，供兄弟服务编译期依赖；无 `distributionManagement`、
  不发外部仓，镜像构建只取 `*-start` 的 fat jar（`docker/Dockerfile`）
- **历史**：git `c706c27` 由单一 `star-site-api` 大契约模块拆回各服务自有 facade，
  包名与 import 零改动——所有 facade 至今共享 `com.xp.starsite.api.*` 基包

### adapter / web —— HTTP 服务端

- **内容**：控制器（`/api/**` C 端 + `/internal/**` 服务间）、`controller/actions/*Actions`
  动作名常量；reminder-web 额外承载 MQ 消费者与定时扫描任务（git `e150bab`：
  「adapter 承载触发器…避免 app/infra 环形依赖，测试随 adapter」）
- **依赖**：`*-common-biz` + `spring-boot-starter-web`（重：传递拉入本服务全链实现）
- **零横切代码**：全局异常处理（`common.exception.GlobalExceptionHandler`）、JWT 拦截器
  （`common.web.JwtAuthInterceptor`）都在 star-site-common，adapter 内无 config/interceptor
- **契约对齐方式**：`/internal/**` 控制器 implements 对应 facade 接口（2026-11-13 起，
  7 个服务全部对齐，签名漂移编译期暴露；此前靠路径约定手工对齐）

## 3. 全服务模块清单

| 服务 | facade | HTTP 对外模块 | 其余模块 |
|---|---|---|---|
| family | family-facade | **family-web** | core-model / dal / core-service / common-biz / start |
| archive | archive-facade | **archive-web** | core-model / dal / core-service / common-biz / start |
| insurance | —（无） | **insurance-web** | core-model / dal / core-service / common-biz / start |
| finance | —（无） | **finance-web** | core-model / dal / core-service / integration / common-biz / start |
| health | health-facade | **health-web**（仓库蓝本） | core-model / dal / integration / core-service / common-biz / start |
| payment | payment-facade | **payment-web**（原 payment-adapter） | core-model / dal / integration / core-service / common-biz / start |
| recheck | —（无） | **recheck-web**（原 recheck-adapter） | core-model / dal / integration / core-service / common-biz / start |
| file | file-facade | **file-web**（原 file-adapter） | core-model / dal / core-service / common-biz / start |
| member | member-facade | **member-web**（原 member-adapter） | core-model / dal / core-service / common-biz / start |
| reminder | reminder-facade | **reminder-web**（原 reminder-adapter） | core-model / dal / core-service / common-biz / start |
| gateway / bench / common | —（非业务服务，不适用） | | gateway 单模块；bench 仅 bench-start |

> 2026-11-13 更新：payment/recheck/file/member/reminder 五个 `*-adapter` 已统一改名 `*-web`，
> 上表右列即改名后现状；§4/§5 中的 `*-adapter` 路径为改名前的证据快照，对应现路径 `*-web`。

## 4. 跨服务 facade 依赖图

### 4.1 Maven 依赖（pom 声明，跨服务）

| facade | 消费方（模块） |
|---|---|
| family-facade | health-common-biz、archive-common-biz、insurance-common-biz、recheck-common-biz、reminder-common-biz、reminder-adapter |
| reminder-facade | health-core-service、health-integration、archive-common-biz、insurance-common-biz、recheck-core-service、recheck-integration |
| health-facade | recheck-core-service、recheck-integration |
| file-facade | health-integration、finance-integration |
| archive-facade | finance-common-biz |
| member-facade | reminder-dal（全仓唯一 DAL 跨服务依赖） |
| payment-facade | **零消费**（pom 注释写明预留给「保险代收/会员订阅」，尚未接线） |

各服务最终 classpath 上的 facade 集：health={health,family,reminder,file}、
recheck={family,reminder,health}、archive={archive,family,reminder}、
insurance={family,reminder}、finance={archive,file}、reminder={reminder,family,member}、
payment={payment}、family/member/file=仅自有。

### 4.2 实际使用方式（三种）

1. **Feign 同步调用**（构造注入，经 `RpcCallGuard.attempt` 容错）：
   - finance `ReportServiceImpl` → `ArchiveApi`（资产汇总）；finance `FileUploadedConsumer` → `FileApi.content()`
   - health `FileIntegration` → `FileApi`；health `PregnancyReminderBridge` → `ReminderApi.upsertRule`
   - recheck `HealthIntegration` → `HealthApi`；recheck `ReminderIntegration` → `ReminderApi`
   - reminder-dal `EmailChannel` → `MemberApi`（取会员邮箱）
2. **MQ 事件 DTO 契约**（不发调用，只共享类型）：
   - archive / insurance 的 common-biz 发布 `ReminderRegisterEvent`/`ReminderCancelEvent`（TOPIC_REMINDER），由 reminder-adapter `ReminderEventConsumer` 消费
   - finance-integration 消费 `FileUploadedEvent`（TOPIC_FILE）
   - health/recheck 的 assembler 用 `ReminderRuleDTO`/`ReminderRepeatType` 组装规则（health-core-service 注释：「纯类型依赖，不发起调用」）
   - payment `PaymentOrderServiceImpl` 发布 `PaymentSucceededEvent`（MQ 停用期消费方以查单兜底）
3. **归属校验 Guard**：health/recheck/archive/insurance/reminder 的 start `WebConfig`
   以 `@Bean familyOwnershipGuard(FamilyApi)` 注册 `FamilyOwnershipGuard`

### 4.3 一个容易误判的点

7 个启用 Feign 的服务都写 `@EnableFeignClients(basePackages = "com.xp.starsite.api.client")`，
由于所有 facade 共享该包，**每个服务会注册其 classpath 上全部 facade 的 Feign bean**
（如 payment 会注册指向自己的 `PaymentInternalApi`），即便从不注入——这是共享基包的副作用，
合并模块会让该问题从「无害冗余 bean」升级为「加载不属于自己的控制器」。

## 5. 合并可行性分析（证据）

### 5.1 环形依赖：6/7 服务直接撞死

典型链（payment，已逐行复核）：

```
payment-common-biz/pom.xml:36  →  payment-facade      （发布 PaymentSucceededEvent，pom:33 注释）
payment-adapter/pom.xml:20     →  payment-common-biz  （控制器调用服务）
```

合并 X = facade + adapter 后：`X → common-biz → X`。同样形状存在于
family / health / reminder / file / member（各自的 common-biz → 自有 facade，
adapter/web → common-biz）。唯一例外 archive 的 `archive-common-biz` 不依赖自有 facade，
但 `archive-web/pom.xml:25` 同时依赖 `archive-common-biz` 与 `archive-facade`，合并后同样自环。

**解环代价**：把事件发布 / DTO 组装从 common-biz 挪进控制器层——违反仓库军规
「控制器零映射」（PaymentInternalController javadoc 明文），且每个服务都要动。

### 5.2 classpath 污染：即使解开环也无法接受

全部 11 个启动类（含 gateway）均为：

```java
@SpringBootApplication(scanBasePackages = "com.xp.starsite")
```

（如 `payment-start/.../PaymentServiceApplication.java:15`、
`finance-start/.../FinanceServiceApplication.java:17`，全仓 11 处已逐一核对）

合并后 `finance-common-biz → archive-facade` 会传递拉入
`archive-web → archive-common-biz → core-service → dal`，finance 进程的组件扫描将实例化
archive 的 `ArchiveInternalController`、service、mapper——幽灵 bean、缺数据表、
`/internal/archive` 路径被错误注册到 finance 端口。要阻止只能在 ~15 个消费方 pom
逐个加 `<exclusions>`，漏一处即生产事故。

### 5.3 结论

facade 轻依赖独立是这套 SOFA/COLA 分层的存在前提：
**契约（facade）与实现（adapter→common-biz→…）必须在不同的 Maven 边界上，
消费方才能只背契约、不背实现。**「都是 HTTP 对外」是对契约两端的重叠直觉，
但两端的服务对象不同：facade 服务的是**编译期的消费方**，adapter 服务的是**运行期的调用方**。

## 6. 现状 anomalies（合并之外值得记下的）

1. `payment-facade` 零消费——契约先行（保险代收/会员订阅），可接受
2. `reminder-dal` 直接依赖 `member-facade`（`EmailChannel` 取邮箱）——全仓唯一「DAL 层跨服务依赖」，
   语义上更像 integration 的活，模块边界上是个例外
3. `archive-common-biz` 不依赖自有 `archive-facade`（自有 facade 只被 `archive-web` 用），
   与其他 6 个服务「common-biz 依赖自有 facade」的惯例不一致
4. `health-facade` 声明了 junit/mockito test 依赖但无任何测试；`family-facade` 的
   `FamilyOwnershipGuardTest` 是全仓唯一 facade 测试
5. 两处本地 `AiApi`（health-integration、finance-common-biz）`@FeignClient(name="star-site-platform-ai")`
   指向本仓不存在的服务（外部契约）
6. integration 依赖方向不统一：payment/health 是 `common-biz → integration`，
   finance 反向 `integration → common-biz`
7. internal 控制器不 implements facade 接口——契约漂移只能靠联调发现（可选改进见 §7）

## 7. 后续演进建议（已确认方向，本文档未实施）

1. **统一 HTTP 对外模块命名为 `*-web`（✅ 已实施，2026-11-13）**：5 个 `*-adapter`（payment/recheck/file/member/reminder）
   已改名 `*-web`——目录、模块 pom artifactId/name/description（对齐「X 域 Web 层」措辞）、
   聚合 pom `<modules>`、start pom 依赖、reminder 聚合 pom 的 COLA 分层注释；
   Java 包名与 import 零改动，docker/deploy/k8s/.github 零引用（构建只认 `*-start` 的 fat jar），
   验证：全仓 reactor 编译通过 + reminder-web（唯一带测试模块）测试通过
2. **可选增强（✅ 已实施，2026-11-13）**：`/internal/**` 控制器 implements 对应 facade 接口
   ——7 服务全部落地（family/health/payment/reminder/file/archive/member；file 的实现类是与
   FileController 同文件的包级私有 FileInternalController；archive 的 summary 方法更名
   assetSummary 对齐契约；member/file 的 @PathVariable 补显式名）。提供方扩展端点
   （health.listReports、reminder.scan）不进契约，留在控制器。控制器保留自身映射注解
   （不依赖 Spring 5.1+ 接口注解继承），编译期保证的是方法签名与返回类型对齐
3. **不建议**在可预见的演进里合并 facade 与 adapter/web（理由见 §5）；
   若未来某 facade 确认永远零消费（如 payment-facade 长期不接线），可评估降级为
   「仅 DTO/事件、无 Feign 接口」甚至并入本服务 common-biz，但那是删除契约而非合并模块
