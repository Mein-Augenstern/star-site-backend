# star-site 统一服务镜像构建（多阶段，不可变基础设施）
# 用法（在仓库根目录）：
#   docker build -f docker/Dockerfile --build-arg MODULE=star-site-platform-member-service -t star-site/member:latest .
# 或直接 docker compose --profile app up -d --build
#
# 国内环境注意：基础镜像 maven:3.9-eclipse-temurin-21 / eclipse-temurin:21-jre
# 需先经加速源拉取并 retag（与 docker-compose.yml 中 RocketMQ 镜像同法）

# ---------- 构建阶段：Maven + JDK 21，Aliyun 镜像源 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# 整仓复制（模块间依赖 parent/common/api，整仓构建最省心）
COPY . .
ARG MODULE
# 整仓构建：reactor 内含 common/api 等内部依赖（容器内 .m2 无历史产物，-pl/-f 单模块取不到兄弟模块）；
# 全部服务镜像共享此 RUN 层缓存：代码不变时仅首次全量，后续镜像构建秒级命中
RUN mvn -q package -DskipTests \
        -s /build/docker/maven-settings.xml

# ---------- 运行阶段：仅 JRE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai \
    JAVA_OPTS=""
# 可观测性注入点：将来挂 OpenTelemetry Agent 只需
#   JAVA_OPTS=-javaagent:/agent/opentelemetry-javaagent.jar（无需改镜像逻辑）
ARG MODULE
# SOFA 多模块服务 jar 位于 <MODULE>/<start模块>/target/（如 health-start）；
# 单模块服务（gateway）不传 JAR_SUB（空 → <MODULE>//target 自动折叠）
ARG JAR_SUB=
COPY --from=build /build/${MODULE}/${JAR_SUB}/target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
