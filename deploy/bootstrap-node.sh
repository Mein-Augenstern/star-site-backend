#!/usr/bin/env bash
# ECS 节点首次初始化（在 ECS 上以 root 执行，或 ssh node-x 'bash -s' < bootstrap-node.sh <profile>）
# 用法: bootstrap-node.sh <node-a|node-b>
set -euo pipefail
PROFILE="${1:?用法: bootstrap-node.sh <node-a|node-b>}"

echo "== 1. 安装 Docker（阿里云镜像源）=="
if ! command -v docker >/dev/null; then
  (dnf -y install dnf-plugins-device 2>/dev/null || yum install -y yum-utils)
  yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
  yum makecache fast && yum install -y docker-ce docker-ce-cli containerd.io
  systemctl enable --now docker
fi
docker version --format '{{.Server.Version}}' || true

echo "== 2. 镜像加速器（ACR 同账号内网免认证）=="
mkdir -p /etc/docker
cat >/etc/docker/daemon.json <<'JSON'
{ "registry-mirrors": ["https://mirror.ccs.tencentyun.com"],
  "log-driver": "json-file", "log-opts": { "max-size": "20m", "max-file": "3" } }
JSON
systemctl restart docker

echo "== 3. 目录与配置 =="
mkdir -p /opt/star-site
# 首次：由本地 scp docker-compose.prod.yml nginx/ mysql-init/ .env.prod 过来
# scp -r deploy/docker-compose.prod.yml deploy/nginx deploy/mysql-init node-a:/opt/star-site/
# scp deploy/.env.prod node-a:/opt/star-site/.env.prod

echo "== 4. swap 1G（2G 内存兜底）=="
if ! swapon --show | grep -q swap; then
  fallocate -l 1G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  grep -q swapfile /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "== 5. ACR 登录（拉镜像用）=="
# docker login <ACR域名> -u <用户名>   # 密码在 ACR 控制台设置固定密码

echo "节点初始化完成（profile=$PROFILE）。部署：IMAGE_TAG=<sha> docker compose -f docker-compose.prod.yml --profile $PROFILE up -d"
