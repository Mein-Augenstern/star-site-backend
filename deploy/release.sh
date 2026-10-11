#!/usr/bin/env bash
# star-site 镜像发布：本地构建 → 推 ACR → SSH 更新节点
# 用法：
#   ./release.sh all                      # 全量（默认 latest 全部服务）
#   ./release.sh gateway biz-health       # 只发指定服务
#   IMAGE_TAG=<sha> ./release.sh all      # 指定 tag（回滚：IMAGE_TAG=<旧sha> all）
# 依赖：docker login <ACR域名>（提前登录一次）；ssh 别名 node-a / node-b（~/.ssh/config）
set -euo pipefail
cd "$(dirname "$0")"

source .env.prod 2>/dev/null || { echo "缺少 .env.prod（复制 .env.prod.example 填写）"; exit 1; }
: "${ACR_PREFIX:?需在 .env.prod 配置 ACR_PREFIX，如 registry.cn-hangzhou.aliyuncs.com/star-site}"

TAG="${IMAGE_TAG:-$(cd .. && git rev-parse --short HEAD)}"
SERVICES="${*:-all}"

# 服务名 → 镜像名 / compose 服务名 / 所在节点
declare -A IMAGE=( \
  [gateway]=gateway [platform-member]=platform-member \
  [biz-family]=family [biz-health]=health [file]=platform-file [reminder]=platform-reminder )
declare -A NODE=( [gateway]=node-a [platform-member]=node-a \
  [family]=node-b [health]=node-b [file]=node-b [reminder]=node-b )
ALL="gateway platform-member family health file reminder"

targets() { [[ "$SERVICES" == all ]] && echo "$ALL" || echo "$SERVICES"; }

echo "== 1/3 构建镜像 (tag=$TAG) =="
for svc in $(targets); do
  mod=$([[ "$svc" == gateway || "$svc" == platform-member ]] && \
    { [[ "$svc" == gateway ]] && echo star-site-platform-gateway || echo star-site-platform-member-service; } || \
    case "$svc" in
      biz-family) echo star-site-biz-family-service;;
      biz-health) echo star-site-biz-health-service;;
      file) echo star-site-platform-file-service;;
      reminder) echo star-site-platform-reminder-service;;
    esac)
  docker build -f Dockerfile --build-arg "MODULE=$mod" \
    -t "$ACR_PREFIX/${IMAGE[$svc]}:$TAG" -t "$ACR_PREFIX/${IMAGE[$svc]}:latest" ..
done

echo "== 2/3 推送 ACR =="
for svc in $(targets); do docker push "$ACR_PREFIX/${IMAGE[$svc]}:$TAG"; done

echo "== 3/3 更新节点 =="
update_node() { # $1=node别名 $2=profile
  echo "--- $1 ($2) ---"
  ssh "$1" "cd /opt/star-site && \
    IMAGE_TAG=$TAG docker compose -f docker-compose.prod.yml --profile $2 pull >/dev/null && \
    IMAGE_TAG=$TAG docker compose -f docker-compose.prod.yml --profile $2 up -d && \
    docker image prune -f"
}
[[ "$SERVICES" == all || " $(targets) " =~ (gateway|platform-member) ]] && update_node node-a node-a
[[ "$SERVICES" == all || " $(targets) " =~ (family|health|file|reminder) ]] && update_node node-b node-b

echo "== 完成：tag=$TAG =="
echo "回滚: IMAGE_TAG=<旧sha> ./release.sh $SERVICES"
