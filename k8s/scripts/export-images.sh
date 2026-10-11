#!/usr/bin/env bash
# 在 Mac A 上执行：导出全部 star-site 服务镜像为单个 tar.gz（无镜像仓库方案的离线搬运）
set -euo pipefail
cd "$(dirname "$0")/../.."

OUT=${1:-/tmp/star-site-images.tar}
IMAGES=(
  star-site/platform-gateway:latest
  star-site/platform-member:latest
  star-site/file:latest
  star-site/reminder:latest
  star-site/biz-family:latest
  star-site/biz-health:latest
  star-site/biz-insurance:latest
  star-site/biz-archive:latest
  star-site/biz-finance:latest
)

echo "==> docker save ${#IMAGES[@]} 个镜像 → $OUT"
docker save "${IMAGES[@]}" -o "$OUT"
gzip -f "$OUT"
echo "完成: $OUT.gz ($(du -h "$OUT.gz" | cut -f1))"
echo "传输: scp $OUT.gz <MacB用户>@<MacB-IP>:/tmp/"
