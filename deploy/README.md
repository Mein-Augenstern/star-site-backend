# 家庭生产部署（家庭自托管 + 公网网关）

> 形态：**ECS 2C2G 只当公网门卫**（Caddy TLS + frp 隧道，占用 <150MB），**两台 16G Mac 跑全栈**（14 容器）。
> 成本：几乎为零（ECS/Mac 已有，frp/Caddy 开源）。数据主权：全部业务数据留在家里 Mac 上。

## 架构

```
iPhone 小程序 / 浏览器
      │ HTTPS（自动证书）
      ▼
┌──────────────────────┐
│ ECS 2C2G（公网门卫）  │  Caddy(TLS, 80/443) ──► frps vhost(8080)
│  - frps  (7000 隧道)  │
│  - Caddy (80/443)     │
└──────────┬───────────┘
           │ frp 隧道（家里 Mac 主动连出，无需公网 IP）
           ▼
┌──────────────────────┐
│ 家里 Mac A（16G）     │  Docker 全栈 14 容器（member/biz-health/biz-finance/reminder/gateway…）
│  - gateway :8080      │  frpc 隧道客户端（compose profile: prod）
│  - 数据全部在此       │
└──────────────────────┘
  Mac B（16G）：备用机——可跑同套 compose 做容灾，或日常关机省电
```

## 硬门槛与前提（按启动顺序）

| # | 事项 | 说明 | 耗时 |
|---|---|---|---|
| 1 | **域名** | 购买一个域名并实名（如 `example.com`） | 当天 |
| 2 | **DNS 解析** | `star.example.com` 与 `api.star.example.com` 两条 A 记录 → ECS 公网 IP | 即时 |
| 3 | **ICP 备案** | 阿里云控制台提交（域名 + 这台 ECS 即满足） | **1-2 周，建议立刻启动**——小程序 request 合法域名硬性要求备案 |
| 4 | ECS 安全组 | 放行 80/443（对外）+ 7000（frp 隧道入） | 1 分钟 |

## 部署步骤

### ECS（公网门卫）

```bash
# 上传 deploy/ecs/ 三件套到 ECS（如 ~/star-gate/）
scp -r deploy/ecs/* user@<ECS-IP>:~/star-gate/
ssh user@<ECS-IP>
cd ~/star-gate

# 1. 改两处配置
vim frps.toml     # auth.token 换强随机值
vim Caddyfile     # star.example.com → 换成你的真实域名（两处）

# 2. 启动
docker compose up -d
```

### 家里 Mac A（全栈）

```bash
cd ~/develop/star-site-backend/docker

# 1. frpc.toml：serverAddr 填 ECS 公网 IP，token 与 frps 一致
vim frpc.toml

# 2. 防睡眠（关键！合盖/休眠 = 服务中断）
sudo pmset -c sleep 0 disksleep 0
# 系统设置 → 电池/能源：插电时防止自动进入睡眠 ✅

# 3. Docker Desktop 设置：登录后自动启动 ✅（General → Start Docker Desktop when you sign in）

# 4. 全栈启动（frpc 随 prod profile 一起起）
docker compose --profile app --profile prod up -d
```

### 验收

```bash
# 家里 Mac 上验证隧道
curl -H "Host: star.example.com" http://127.0.0.1:8080/api/auth/login -X POST \
  -H 'Content-Type: application/json' -d '{"username":"x","password":"x"}'
# 期望：返回业务 JSON（说明 隧道→网关→服务 全通）

# 外网验证（手机蜂窝网络）
# 浏览器访问 https://star.example.com → 小程序后台配 request 合法域名 → 真机可用
```

## Mac 常驻注意事项

- **防睡眠**：`sudo pmset -c sleep 0 disksleep 0`（插电不休眠）；系统设置里关掉"硬盘休眠"
- **开机自启**：Docker Desktop 开机自启 ✅；compose 服务 `restart: unless-stopped` ✅
- **系统自动更新**：设置自动更新为"下载后询问安装"（避免半夜自动重启）
- **数据备份**：家里 Mac 的 MySQL 数据（docker volume mysql-data）每周 `docker exec star-mysql mysqldump` 备份到 Mac B 或移动硬盘
- **电费/噪音**：Mac 笔记本 24h 运行功耗约 5-15W，无风扇噪音，可接受

## Mac B 容灾（可选，后期）

frpc 支持同名 proxy 的**负载均衡/故障切换**：Mac B 跑同一套 compose + frpc（同 proxy name），frps 侧自动流量分摊；Mac A 故障时流量切 B。
前提：MySQL 数据主从同步（或定期 mysqldump 恢复到 B）——建议放 V2，先把单机跑稳。

## 与小程序的衔接

- 微信后台 → 开发管理 → 服务器域名 → request 合法域名填 `https://api.star.example.com`
- 小程序 `request.ts` 的 BASE_URL 改为 `https://api.star.example.com/api`
- 备案通过前：真机"预览"仍可用（不校验域名），正式体验版需等备案
