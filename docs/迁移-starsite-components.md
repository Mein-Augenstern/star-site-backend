# star-site-backend → star-site-components 迁移说明

> 2026-10-25：`star-site-common` 内置的雪花 ID 与异步命令实现已替换为
> [star-site-components](https://github.com/Mein-Augenstern/star-site-components)（`0.1.0-SNAPSHOT`）。
> 表结构（`sys_sequence` / `async_command`）与语义完全兼容，**零 DDL、零数据迁移**。

## 迁移了什么

| 原 common 内置（已删除） | 替换为组件 | 行为变化 |
|---|---|---|
| `common/id/SequenceWorkerIdAllocator`（自研租约） | `starsite-sequence` 的 `MysqlWorkerIdAllocator` | 无（同一 `__global__` 池 + LAST_INSERT_ID 语义；租约失败回绕改为 RESET 告警而非清零，更安全） |
| `common/util/IdGenerator`（自研雪花） | 同类名**薄委托门面** → `SnowflakeIdGenerator` | API 不变（`nextId()`/`new IdGenerator(workerId)`）；时钟回拨保持钳制策略 |
| `common/async/`（8 文件：模型/服务/调度/Mapper/管控） | `starsite-async-command` + starter 自动装配 | 见下"差异清单" |

## 部署与配置

**开关映射**（`STAR_ASYNC_ENABLED` 部署变量继续生效，health yml 已做兼容映射）：

```yaml
starsite:
  async-command:
    enabled: ${STAR_ASYNC_ENABLED:true}     # 原 @Import(AsyncCommandConfig) + STAR_ASYNC_ENABLED
    admin-token: ${STARSITE_ASYNC_ADMIN_TOKEN:}  # 新：管控端点鉴权（推荐配置）
```

**管控端点鉴权变化**：原 `star.async.admin-member-ids`（memberId 白名单）已移除，
改为组件三级鉴权——自定义 `AsyncCommandAdminGuard` Bean / `admin-token` 内置守卫
（请求头 `X-Starsite-Admin-Token`）/ 无守卫放行+启动 WARN。需要按 memberId 鉴权的，
实现 Guard Bean 从请求属性取登录态即可。

**构建依赖**：组件为 `0.1.0-SNAPSHOT`，本地构建需先 `mvn install` 组件仓
（`star-site-components`）；后续发布 GitHub Packages / 中央仓后改正式版本。

**编译环境**：本仓 Lombok 1.18.30 支持到 JDK 21——用 temurin-21 构建
（`docker run ... maven:3.9-eclipse-temurin-21 mvn verify`），JBR 25 会静默丢注解处理。

## 差异清单（原内置 → 组件）

1. **健康端点**：引 actuator 的服务自动获得 `starsiteAsyncCommand` 指示器
   （死信即 DOWN）——原内置无此能力。
2. **指标**：classpath 有 micrometer 时自动打点（命令四态计数 + 执行耗时 Timer）。
3. **调度线程**：组件自带 `starsite-async-cmd` 单线程调度（SmartLifecycle 托管，
   容器最后启动/最先停止），`@EnableScheduling` 不再是组件前提（health 保留是自有扫描任务用）。
4. **批量执行器**：新增 `AsyncCommandBatchExecutor`（同轮同类型合并单次调用），
   health 的 `AiInterpretExecutor` 保持单条实现，后续可按需升级批量。
5. **digest 日志**：原 `AsyncCommandService` 的 task-digest 摘要日志由组件标准
   slf4j 日志替代（info/warn/error 分级语义一致，字段略有精简）。
6. **管控端点路径**：默认 `/api/admin/async` 不变；处置动作新增 requeue（悬挂打回）。

## 业务代码改动（全部在 health 服务）

- `AiInterpretExecutor`：import 从 `common.async` → `com.xp.starsite.asynccmd`
- `ReportInsightServiceImpl`：`AsyncCommandService` → `AsyncCommandClient`（方法同名零改）
- `HealthServiceApplication`：去掉 `@Import(AsyncCommandConfig.class)`（starter 自动装配）
- 其他 10 个服务的 `IdGenerator.nextId()` 调用**零改动**（门面保留原 API）
