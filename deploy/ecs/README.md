# ECS 侧部署（2C2G 只当公网门卫：Caddy TLS 入口 + frp 服务端）

> 职责：TLS 终结（自动证书）→ 按域名路由 → frp 隧道 → 家里 Mac 的网关容器。
> 资源占用：Caddy + frps 合计 < 150MB，2C2G 绰绰有余。

## 前提（一次性，需要你操作）

1. **域名**：购买一个域名（如 example.com），DNS 解析两条 A 记录到 ECS 公网 IP：
   - `star.example.com`（主入口/H5）
   - `api.star.example.com`（小程序 request 合法域名）
2. **ICP 备案**：阿里云控制台提交备案（域名 + 这台 ECS 即满足条件，流程 1-2 周）。
   **这是全链路唯一的硬门槛，建议立刻启动**（小程序 request 合法域名硬性要求备案）。
3. **安全组放行**：80/TCP、443/TCP（对外）；7000/TCP（frp 隧道，家里 Mac 主动连入）
4. **frp 版本**：若 ECS 上有旧版 frp，建议直接用下方容器方案覆盖（配置格式以 frp ≥0.52 TOML 为准）

## 文件

- `docker-compose.yml`：caddy + frps 两个容器
- `Caddyfile`：TLS + 域名路由
- `frps.toml`：frp 服务端（token 认证）

## 启动

```bash
docker compose up -d
```

## 安全说明

- frps 仅监听本机回环 + 隧道端口，dashboard 不对公网暴露
- token 必须换成强随机值（frpc.toml 同步）
- ECS 上**不存任何业务数据**——数据都在家里 Mac 上
