#!/usr/bin/env bash
# 一次性恢复：物化 HEAD 全树 + 父 pom 引用修正 + 构建 + 提交锁定
set -uo pipefail
R=/Users/xiaopu/develop/star-site-backend

echo "== 1. 物化 HEAD 全树 =="
git -C "$R" checkout HEAD -- .
find "$R" -name '*.java' -not -path '*/target/*' | wc -l

echo "== 2. 父 pom 引用修正（biz-service/app → core-service）=="
python3 - <<'PYEOF'
import os
R = '/Users/xiaopu/develop/star-site-backend'
RENAMES = {
    'family-biz-service': 'family-core-service',
    'insurance-biz-service': 'insurance-core-service',
    'archive-biz-service': 'archive-core-service',
    'finance-biz-service': 'finance-core-service',
    'member-app': 'member-core-service',
    'file-app': 'file-core-service',
    'reminder-app': 'reminder-core-service',
}
n = 0
for root, dirs, files in os.walk(R):
    dirs[:] = [d for d in dirs if d not in ('target', '.git', 'node_modules')]
    for f in files:
        if not f.endswith('.xml'):
            continue
        p = os.path.join(root, f)
        s = open(p).read()
        orig = s
        for old, new in RENAMES.items():
            s = s.replace('<artifactId>' + old + '</artifactId>', '<artifactId>' + new + '</artifactId>')
            s = s.replace('<module>' + old + '</module>', '<module>' + new + '</module>')
        if s != orig:
            open(p, 'w').write(s)
            n += 1
print('修正 pom 数:', n)
PYEOF

echo "== 3. 构建 =="
cd "$R"
export JAVA_HOME=$HOME/develop/jdk-21/Contents/Home
"$HOME/develop/apache-maven-3.9.9/bin/mvn" install -s docker/maven-settings.xml > /tmp/fb5.log 2>&1
echo "BUILD EXIT=$?"
grep -E 'BUILD' /tmp/fb5.log | tail -1
echo "零失败测试模块数: $(grep -cE 'Tests run.*Failures: 0' /tmp/fb5.log)"

echo "== 4. 提交锁定 =="
git -C "$R" add -A
git -C "$R" commit -m "refactor(arch): 全服务模块统一 core-service 命名——biz-service/app 重命名落地（7 服务父 pom 引用同步）；worktree 从 32592d4 完整物化" 2>&1 | tail -1
git -C "$R" log --format='%h %s' -1
