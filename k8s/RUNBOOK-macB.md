# RUNBOOK：Mac B（32G）k3s 集群落地与切流

> 目标形态：Mac B 跑 k3s（全部无状态服务），Mac A 只跑有状态基础设施（MySQL/Redis/Nacos/RocketMQ，compose 不动），ECS 门卫不变。
> 全程 Mac A 现役 compose 栈**不下线**，k3s 侧验收通过后只改 frpc 一个地址完成切流，随时改回。

## 0. 前置（Mac B 一次性）

```bash
brew install multipass helm kubectl
```

Mac A 的基础设施对 k3s Pod 暴露依赖两个前提：
- 路由器把 Mac A 绑定静态 IP（当前 192.168.3.196，与 k8s/helm/*/values.yaml `infra.host` 一致）
- Mac A 防火墙放行 3307/8848/6379/9876（macOS 一般默认放行局域网入站；不通再检查）

## 1. 起集群（Mac B）

```bash
cd k8s/scripts && ./install-k3s-macB.sh
# 记下输出里的 VM IP，如 192.168.3.x
export KUBECONFIG=~/.kube/star-k3s
kubectl get nodes   # Ready 即成
```

## 2. 搬镜像（Mac A → Mac B，免镜像仓库）

```bash
# Mac A
k8s/scripts/export-images.sh           # 产出 /tmp/star-site-images.tar.gz
scp /tmp/star-site-images.tar.gz <macB用户>@<macB-ip>:/tmp/
# Mac B
k8s/scripts/import-images.sh /tmp/star-site-images.tar.gz
```

## 3. 部署（Mac B）

```bash
KUBECONFIG=~/.kube/star-k3s k8s/scripts/deploy.sh
# Pods 全 Running 后，集群内自测：
kubectl -n star-site run curl --rm -it --image=curlimages/curl --restart=Never -- \
  curl -s -X POST http://star-gateway:8080/api/auth/wx-login \
  -H 'Content-Type: application/json' -d '{"code":"k3s-smoke"}'
# 期望 {"code":0,...}
```

注意：values.yaml `infra.host` 指 Mac A；`ingress.host` 先用占位域名，局域网测试可在测试机 hosts 里把该域名指到 VM IP。

## 4. 切流（Mac A，唯一改动点）

编辑 `docker/frpc.toml`，把隧道目标从本机 8080 改为 k3s VM 的 Traefik 入口：

```toml
[[proxies]]
name = "star-gateway-tcp"
type = "tcp"
localIP = "<VM-IP>"      # 原 127.0.0.1
localPort = 80           # Traefik web entrypoint；原 8080
remotePort = 9090
```

重启隧道：`docker compose restart frpc`，然后 iPhone 走公网回归（登录/报告/孕期）。
**回退**：localIP 改回 `127.0.0.1`、localPort 改回 `8080`，`docker compose restart frpc`，30 秒内恢复。

## 5. P4 可观测栈安装（Mac B）

values：`k8s/helm/observability-values.yaml`（家用单节点资源调优：Prometheus 3 天滚动、Grafana NodePort 30300、Loki 72h）。

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  -n observability --create-namespace -f k8s/helm/observability-values.yaml

helm upgrade --install loki grafana/loki-stack -n observability -f k8s/helm/observability-values.yaml
```

然后打开服务侧抓取：两族 values `serviceMonitor.enabled: true` → 重跑 `deploy.sh`。
验收：`kubectl -n star-site get servicemonitors` 有 9 个；Grafana（http://<VM-IP>:30300，admin / star-site-grafana）导入 JVM(Micrometer) 看板、Explore 里查 Loki 日志。

## 6. 常态运维

- 升级服务：Mac A 改代码 → 本地 `mvn package` → `docker build` 新 tag → `export-images.sh` → Mac B `import` → 改 values tag → `deploy.sh`（helm 原地滚动）。接入 ACR 后可省搬运步骤（见技术文档 09 可选项）。
- Mac B 重启后：`multipass start star-k3s` 即可（k3s 随 VM 自启）。
- 磁盘：VM 40G 起，镜像重复导入注意 `k3s ctr images prune`（或 `crictl rmi --prune`）。

## 7. 与旧 k8s/*.yaml 的关系

`00-04` 裸 manifests 是早期雏形，已被 Helm charts 取代，仅留档参考；后续只维护 `k8s/helm/`。
