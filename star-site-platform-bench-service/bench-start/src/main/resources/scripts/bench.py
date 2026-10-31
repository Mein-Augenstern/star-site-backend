#!/usr/bin/env python3
"""
三组件 RPC 层压测脚本：向 bench-service 发送并发 HTTP 请求，测量端到端延迟百分位。
组件升级后重新部署 bench-service 即可复用本脚本验证性能。

用法：
  python3 bench.py --url http://localhost:18090 --scenario all
  python3 bench.py --url http://localhost:18090 --scenario id --concurrency 16
  python3 bench.py --url http://localhost:18090 --scenario lock,async
  python3 bench.py --url http://localhost:18090 --scenario all --quick
"""
import argparse
import concurrent.futures
import time
import urllib.request
import urllib.error

def get(url):
    t0 = time.perf_counter_ns()
    with urllib.request.urlopen(urllib.request.Request(url), timeout=10) as resp:
        resp.read()
    return time.perf_counter_ns() - t0

def post(url):
    t0 = time.perf_counter_ns()
    req = urllib.request.Request(url, data=b"", method="POST")
    with urllib.request.urlopen(req, timeout=10) as resp:
        resp.read()
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
    parser = argparse.ArgumentParser(description="starsite components RPC benchmark")
    parser.add_argument("--url", default="http://localhost:18090", help="bench-service base URL")
    parser.add_argument("--scenario", default="all", help="id/lock/lock-wait/async/async-idempotent/all")
    parser.add_argument("--concurrency", type=int, default=None, help="override concurrency (default: 1,8,16)")
    parser.add_argument("--requests", type=int, default=None, help="override total requests per scenario")
    parser.add_argument("--quick", action="store_true", help="quick mode (fewer requests)")
    args = parser.parse_args()

    # 健康检查
    try:
        resp = get(f"{args.url}/api/bench/health")
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
