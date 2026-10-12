#!/usr/bin/env bash
# 全仓构建 + 关键模块文件数核验（只读检查，无源码写入）
R=/Users/xiaopu/develop/star-site-backend
for d in star-site-biz-family-service/family-biz-service star-site-platform-member-service/member-app star-site-biz-health-service/health-core-service; do
  n=$(find "$R/$d" -name '*.java' 2>/dev/null | wc -l | tr -d ' ')
  echo "$d: $n java"
done
cd "$R"
export JAVA_HOME=$HOME/develop/jdk-21/Contents/Home
"$HOME/develop/apache-maven-3.9.9/bin/mvn" install -s docker/maven-settings.xml > /tmp/full_build3.log 2>&1
echo "EXIT=$?"
grep -E 'BUILD' /tmp/full_build3.log | tail -1
echo "零失败测试模块数: $(grep -cE 'Tests run.*Failures: 0' /tmp/full_build3.log)"
