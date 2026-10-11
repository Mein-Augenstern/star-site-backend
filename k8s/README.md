# star-site K8s 部署清单（凤凰架构"后微服务"形态）

> 前提：集群内有 MySQL（star-mysql:3306）/ Nacos（star-nacos:8848）/ RocketMQ（star-rocketmq-namesrv:9876）
> ——可以直接把 docker-compose 的基础设施 `network_mode` 换成 NodePort 暴露，或整体迁入集群。
> 所有应用配置已环境变量化（云原生演进 #4），此处 ConfigMap/Secret 注入即可，Java 代码零改动。

## 文件

| 文件 | 内容 |
|---|---|
| 00-namespace-config.yaml | namespace + ConfigMap（公共 env）+ Secret（JWT/DB 密码） |
| 01-data-services.yaml | member/family/file/health 四个平台的 Deployment + Service |
| 02-biz-services.yaml | insurance/reminder/archive/finance 四个业务域的 Deployment + Service |
| 03-gateway.yaml | 网关 Deployment + NodePort Service（对外入口） |

## 部署

```bash
# 1. 镜像就位（集群可拉取：本地构建后推仓库，或 k3s 使用本地 import）
docker build -f docker/Dockerfile --build-arg MODULE=star-site-platform-gateway -t star-site/gateway:latest .
# ... 其余 7 个服务同法
k3s ctr images import ...   # 或 docker save | k3s ctr images import

# 2. 应用清单
kubectl apply -f k8s/
```

## 凤凰架构对齐说明

- 服务发现仍用 Nacos（Spring Cloud 模式）；下一步可切 Spring Cloud Kubernetes Discovery，
  用 K8s Service/DNS 替代 Nacos，届时删除 Nacos 依赖仅改 env（见 06-云原生演进方案 第四节）
- readiness/liveness 探针用 `/actuator/health`（所有服务已具备）
- 资源限额对齐小内存经验值（每服务 320-384Mi）
