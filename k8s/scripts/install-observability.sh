#!/usr/bin/env bash
# P4 可观测栈一键安装（Mac B，k3s 集群内）：kube-prometheus-stack + Loki/Promtail
# 前提：helm + kubectl + KUBECONFIG 就绪；Chart 从国内可达的官方仓库拉取
# 用法：./install-observability.sh [--with-loki]
set -euo pipefail

NS=observability
WITH_LOKI=${1:-}

helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

VALUES="$(cd "$(dirname "$0")/../helm" && pwd)/observability-values.yaml"

echo "==> kube-prometheus-stack"
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  -n $NS --create-namespace -f "$VALUES"

if [ "$WITH_LOKI" = "--with-loki" ]; then
  echo "==> loki-stack（Loki+Promtail）"
  helm upgrade --install loki grafana/loki-stack \
    -n $NS -f "$VALUES"
fi

echo "==> 打开 star-site 的 ServiceMonitor（两族 chart 各置 true 后 deploy.sh 重部署）"
echo "    kubectl -n star-site get servicemonitors"
echo "Grafana: http://<VM-IP>:30300 （admin / star-site-grafana）"
echo "Loki 已随 loki-stack 装好时，Grafana 数据源默认已注入（loki isDefault）。"
