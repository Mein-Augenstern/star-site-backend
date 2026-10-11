#!/usr/bin/env bash
# 在 Mac B 上执行：multipass 起 Ubuntu VM + 安装 k3s + 导出 kubeconfig
# 前提：brew install multipass；VM 规格 4C8G（32G 机器余量充足）
set -euo pipefail

VM_NAME=${VM_NAME:-star-k3s}
VM_CPU=${VM_CPU:-4}
VM_MEM=${VM_MEM:-8G}
VM_DISK=${VM_DISK:-40G}

echo "==> 1/4 启动 multipass VM（首次会下载 Ubuntu 镜像）"
multipass launch --name "$VM_NAME" --cpus "$VM_CPU" --memory "$VM_MEM" --disk "$VM_DISK" 22.04 || true

echo "==> 2/4 VM 内安装 k3s（国内源，跳过 Traefik 由 k3s 自带）"
multipass exec "$VM_NAME" -- bash -c 'curl -sfL https://rancher-mirror.rancher.cn/k3s/k3s-install.sh | INSTALL_K3S_MIRROR=cn sh -s - --write-kubeconfig-mode 644'

echo "==> 3/4 取 kubeconfig 到本机 ~/.kube/star-k3s"
mkdir -p ~/.kube
multipass exec "$VM_NAME" -- sudo cat /etc/rancher/k3s/k3s.yaml \
  | sed "s|127.0.0.1|$(multipass info "$VM_NAME" | grep IPv4 | awk '{print $2}')|" \
  > ~/.kube/star-k3s

echo "==> 4/4 验证"
export KUBECONFIG=~/.kube/star-k3s
kubectl get nodes -o wide
echo "完成。后续命令带 KUBECONFIG=~/.kube/star-k3s 或直接 export。"
echo "VM IP: $(multipass info "$VM_NAME" | grep IPv4 | awk '{print $2}')  ← frpc.toml 的隧道目标要指向它（Traefik 80 端口）"
