# star-site K8s 部署（Helm 为主，裸 manifests 已废弃留档）

## 当前部署路径

| 路径 | 说明 |
|---|---|
| `helm/star-site-platform/` | gateway/member/file/reminder（含 Traefik Ingress、ServiceMonitor 开关） |
| `helm/star-site-biz/` | family/health/insurance/archive/finance（health/finance 带 LLM env） |
| `scripts/install-k3s-macB.sh` | Mac B：multipass VM + k3s 安装 + kubeconfig 导出 |
| `scripts/export-images.sh` / `import-images.sh` | 免镜像仓库的镜像离线搬运（Mac A → Mac B） |
| `scripts/deploy.sh` | Helm 部署/升级两族 |
| `RUNBOOK-macB.md` | **从零到切流完整手册（含 frpc 切换点与回退）** |

环境契约与 compose `x-app-env` 等价，基础设施留在 Mac A（MySQL 3307 / Nacos 8848 / Redis 6379 / RocketMQ 9876），values.yaml `infra.host` 指向 Mac A 局网 IP。

## 旧裸 manifests（已废弃）

`00-namespace-config.yaml ~ 04-*.yaml` 为早期雏形，被 Helm charts 取代，仅留档参考，勿再更新。

## 云原生对齐说明

- 服务发现仍用 Nacos（Spring Cloud 模式）；后续可切 Spring Cloud Kubernetes Discovery 用 K8s DNS 替代（见 star-site-docs/技术/09 可选项路线）
- readiness/liveness 探针走 `/actuator/health`；`/actuator/prometheus` 供 P4 抓取（micrometer-registry-prometheus 已进各 start 模块）
- 资源限额每服务 512Mi（Pod 内 JVM -Xmx256m）
