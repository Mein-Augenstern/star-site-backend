#!/usr/bin/env bash
# Helm 部署/升级两族服务（platform 先于 biz，gateway 就位后 biz 再注册 nacos）
# 用法：KUBECONFIG=~/.kube/star-k3s ./deploy.sh [values 覆盖文件...]
set -euo pipefail
cd "$(dirname "$0")/../helm"

NS=star-site
EXTRA_VALUES=()
for f in "$@"; do EXTRA_VALUES+=(-f "$f"); done

echo "==> namespace"
kubectl create namespace $NS --dry-run=client -o yaml | kubectl apply -f -

echo "==> platform 族（gateway/member/file/reminder）"
helm upgrade --install star-site-platform ./star-site-platform \
  -n $NS --create-namespace "${EXTRA_VALUES[@]}"

echo "==> biz 族（family/health/insurance/archive/finance）"
helm upgrade --install star-site-biz ./star-site-biz \
  -n $NS "${EXTRA_VALUES[@]}"

echo "==> 等待就绪"
kubectl -n $NS wait --for=condition=available --timeout=300s deploy --all || true
kubectl -n $NS get pods -o wide
