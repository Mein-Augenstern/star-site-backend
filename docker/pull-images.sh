#!/bin/bash
# 多源轮询拉取 Docker 镜像并 retag 为官方名（compose 为 pull_policy: never）
# 2026-10-06 探测：daemon.json 里 4 个老源（ustc/163/baidu/docker-cn）已全部停服，
# 不要再配；以下为本次 HTTP /v2/ 探活通过的源，按"未试过的新源优先"排序。
MIRRORS=(
  "docker.1panel.live"
  "dockerproxy.net"
  "docker.hlmirror.com"
  "hub.rat.dev"
  "docker.aityp.com"
  "docker.m.daocloud.io"
  "docker.1ms.run"
)

# 依赖清单：RocketMQ + 全栈容器化所需基础镜像
IMAGES=(
  "apache/rocketmq:5.3.1"
  "maven:3.9-eclipse-temurin-21"
  "eclipse-temurin:21-jre"
)

for image in "${IMAGES[@]}"; do
  # 已有官方名镜像则跳过
  if docker image inspect "$image" > /dev/null 2>&1; then
    echo "=== SKIP (exists): $image ==="
    continue
  fi
  ok=""
  for m in "${MIRRORS[@]}"; do
    echo "=== [$image] trying $m ==="
    if docker pull "$m/$image"; then
      docker tag "$m/$image" "$image"
      docker rmi "$m/$image" > /dev/null 2>&1
      echo "=== SUCCESS: $image via $m ==="
      ok=1
      break
    fi
  done
  [ -z "$ok" ] && echo "=== FAILED: $image (all mirrors) ==="
done
echo "=== ALL DONE ==="
