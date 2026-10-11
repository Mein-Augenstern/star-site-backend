#!/usr/bin/env bash
# 在 Mac B 上执行：把镜像 tar.gz 导入 k3s 容器运行时（containerd）
# 用法：./import-images.sh /tmp/star-site-images.tar.gz
set -euo pipefail

TAR=${1:?用法: $0 /tmp/star-site-images.tar.gz}
VM_NAME=${VM_NAME:-star-k3s}

echo "==> 传入 VM 并导入 k3s（gz 需先解压，ctr 不认 gzip）"
gunzip -kf "$TAR"
RAW="${TAR%.gz}"
multipass transfer "$RAW" "$VM_NAME:/tmp/"
multipass exec "$VM_NAME" -- bash -c "sudo k3s ctr images import /tmp/$(basename "$RAW") && sudo k3s ctr images ls | grep star-site"
echo "完成。"
