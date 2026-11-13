#!/usr/bin/env python3
"""
三组件 RPC 层压测脚本：向 bench-service 发送并发 HTTP 请求，测量端到端延迟百分位。
组件升级后重新部署 bench-service 即可复用本脚本验证性能。

用法（自持压测环境需显式 --allow-private 放行环回/内网目标）：
  python3 bench.py --url http://localhost:18090 --allow-private --scenario all
  python3 bench.py --url http://localhost:18090 --allow-private --scenario id --concurrency 16
  python3 bench.py --url http://localhost:18090 --allow-private --scenario lock,async
  python3 bench.py --url http://localhost:18090 --allow-private --scenario all --quick
"""
import argparse
import concurrent.futures
import ipaddress
import socket
import time
import urllib.error
import urllib.parse
import urllib.request

_ALLOW_PRIVATE = False  # main() 里按 --allow-private 设置

def _check_scheme(url):
    """逐请求校验（无 DNS 开销）：仅允许 http/https。"""
    scheme = urllib.parse.urlsplit(url).scheme
    if scheme not in ("http", "https"):
        raise ValueError(f"仅允许 http/https 协议: {url}")

def validate_target(url):
    """入口整校验（含 DNS 解析）：拒绝环回/私有/保留/链路本地地址，防 SSRF 打内网。

    --allow-private 为自持压测环境（本机/内网 ECS 上自部署的 bench-service）的显式放行。
    """
    parts = urllib.parse.urlsplit(url)
    if parts.scheme not in ("http", "https"):
        raise ValueError(f"仅允许 http/https 协议: {url}")
    if not parts.hostname:
        raise ValueError(f"URL 缺少 host: {url}")
    port = parts.port or (443 if parts.scheme == "https" else 80)
    addrs = {sockaddr[0] for _, _, _, _, sockaddr in
             socket.getaddrinfo(parts.hostname, port, proto=socket.IPPROTO_TCP)}
    for raw in addrs:
        ip = ipaddress.ip_address(raw)
        if ip.is_loopback or ip.is_private or ip.is_reserved or ip.is_link_local \
                or ip.is_multicast or ip.is_unspecified:
            if not _ALLOW_PRIVATE:
                raise ValueError(f"目标 {parts.hostname} 解析到内网/环回/保留地址 {ip}；"
                                 f"自持压测环境请显式加 --allow-private")
            print(f"  ⚠️ --allow-private 已放行非公网目标: {parts.hostname} ({ip})")

def _open(url, method="GET"):
    _check_scheme(url)
    req = urllib.request.Request(url, data=b"" if method == "POST" else None, method=method)
    with urllib.request.urlopen(req, timeout=10) as resp:
        resp.read()

def get(url):
    t0 = time.perf_counter_ns()
    _open(url, "GET")
    return time.perf_counter_ns() - t0

def post(url):
    t0 = time.perf_counter_ns()
    _open(url, "POST")
    return time.perf_counter_ns() - t0

def fmt(ns):
    if ns > 1e6: return f"{ns/1e6:.1f}ms"
    if ns > 1e3: return f"{ns/1e3:.0f}μs"
    return f"{ns}ns"

def bench(name, method, url, total, concurrency):
    fn = get if method == "GET" else post
    latencies, errors = [], 0
    start = time.time()
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as pool:
        futures = [pool.submit(fn, url) for _ in range(total)]
        for f in concurrent.futures.as_completed(futures):
            try: latencies.append(f.result())
            except Exception: errors += 1
    elapsed = time.time() - start
    latencies.sort()
    if not latencies:
        print(f"  ❌ {name}: all {total} requests failed")
        return
    ops = len(latencies) / elapsed
    p = lambda q: latencies[min(int(len(latencies) * q), len(latencies)-1)]
    status = "✅" if errors == 0 else f"⚠️ {errors} errors"
    print(f"  {status} {name:45s} {concurrency:3d} thr | {ops:>8,.0f} ops/s | P50={fmt(p(0.50)):>8s} P95={fmt(p(0.95)):>8s} P99={fmt(p(0.99)):>8s} max={fmt(latencies[-1]):>8s}")

def main():
    global _ALLOW_PRIVATE
    parser = argparse.ArgumentParser(description="starsite components RPC benchmark")
    parser.add_argument("--url", default="http://localhost:18090", help="bench-service base URL")
    parser.add_argument("--scenario", default="all", help="id/lock/lock-wait/async/async-idempotent/all")
    parser.add_argument("--concurrency", type=int, default=None, help="override concurrency (default: 1,8,16)")
    parser.add_argument("--requests", type=int, default=None, help="override total requests per scenario")
    parser.add_argument("--quick", action="store_true", help="quick mode (fewer requests)")
    parser.add_argument("--allow-private", action="store_true",
                        help="显式放行环回/私有目标（自持压测环境；默认拒绝防 SSRF）")
    args = parser.parse_args()
    _ALLOW_PRIVATE = args.allow_private

    # 目标整校验（scheme + DNS 解析后的地址段检查）
    try:
        validate_target(args.url)
    except ValueError as e:
        print(f"❌ 目标校验失败: {e}")
        return

    # 健康检查
    try:
        get(f"{args.url}/api/bench/health")
        print(f"✅ bench-service 就绪: {args.url}")
    except Exception as e:
        print(f"❌ bench-service 不可达: {args.url} ({e})")
        return

    scenarios = args.scenario.split(",")
    run_all = "all" in scenarios
    concurrencies = [args.concurrency] if args.concurrency else [1, 8, 16]
    base_requests = args.requests or (500 if args.quick else 2000)

    print(f"\n{'='*80}")
    print(f"  starsite components RPC benchmark")
    print(f"  Target: {args.url} | Scenarios: {args.scenario}")
    print(f"{'='*80}")

    if run_all or "id" in scenarios:
        print(f"\n--- sequence: snowflake ID ---")
        for c in concurrencies:
            bench(f"ID gen", "GET", f"{args.url}/api/bench/id", base_requests, c)

    if run_all or "lock" in scenarios:
        print(f"\n--- lock: acquire/release ---")
        lock_total = base_requests // 4  # lock is slower
        for c in concurrencies:
            bench(f"lock acquire/release", "GET", f"{args.url}/api/bench/lock?name=bench", lock_total, c)

    if run_all or "lock-wait" in scenarios:
        print(f"\n--- lock: timed wait ---")
        for c in concurrencies:
            bench(f"lock wait 100ms", "GET", f"{args.url}/api/bench/lock-wait?name=bench&waitMs=100", base_requests // 4, c)

    if run_all or "async" in scenarios:
        print(f"\n--- async-command: submit ---")
        async_total = base_requests // 4
        for c in concurrencies:
            bench(f"async submit", "POST", f"{args.url}/api/bench/async?userId=bench&message=test", async_total, c)

    if run_all or "async-idempotent" in scenarios:
        print(f"\n--- async-command: idempotent submit ---")
        for c in concurrencies:
            bench(f"async idempotent", "POST", f"{args.url}/api/bench/async-idempotent?userId=bench", base_requests // 4, c)

    print(f"\n{'='*80}")

if __name__ == "__main__":
    main()
