# XTV 实施与部署手册

本文说明如何把本仓库部署到可对外服务的环境，以及 **USDT 链上收款是如何接入、如何对账开通** 的。按步骤执行即可。

---

## 0. 系统是什么

| 部分 | 技术 | 职责 |
|------|------|------|
| `apps/web` | React + Vite + Nginx | 前台浏览/播放/下单，后台管理界面 |
| `apps/api` | Spring Boot 3 / Java 17 / JPA / Flyway | 鉴权、目录、媒体签名、订单、链上对账、转码任务 |
| MySQL 8（生产）或 H2（本地） | 持久化 | 用户、影片、订单、平台配置等 |
| 本地盘或 S3 兼容对象存储 | 片源/封面/HLS | 上传与分发 |
| FFmpeg（API 容器内） | 转码 | 片源 → HLS |

**支付模式：无托管商收银台。** 用户向平台配置的链上收款地址转 **精确金额** 的 USDT；服务端轮询 TronGrid / BSC / 其它链节点 API，金额匹配后入账并开通会员或单集权益。

---

## 1. 部署前准备

### 1.1 机器与软件

- Linux x86_64 或 ARM（需能跑 Docker）
- Docker + Docker Compose v2
- 公网域名（建议）+ HTTPS 证书（Let’s Encrypt / 云厂商）
- 出网访问：
  - `https://api.trongrid.io`（或 Nile：`https://nile.trongrid.io`）
  - BSC / 其它链 RPC（若启用对应渠道）

### 1.2 准备密钥与账号（生产必填）

| 变量 | 说明 |
|------|------|
| `XTV_JWT_SECRET` | ≥32 字符随机串，**禁止**用仓库默认值 |
| `XTV_ADMIN_USERNAME` / `XTV_ADMIN_PASSWORD` | 后台管理员；密码**禁止**用 `admin123` |
| `XTV_CORS_ORIGINS` | 前台站点 Origin，如 `https://www.example.com` |
| `SPRING_DATASOURCE_*` | MySQL 连接（Compose 已给默认，生产请改强密码） |
| Tron API Key（可选但推荐） | TronGrid 申请，填到后台「收款方式」 |
| 收款地址 | TRC20 / BEP20 等，**主网或测试网与后台网络选项一致** |

生产启动时 `ProdSecurityGuard` 会强制校验：JWT、管理员密码、且 `payment-simulate=false`。

### 1.3 克隆与目录

```bash
git clone <你的仓库> xtv
cd xtv
mkdir -p data/media
```

媒体卷默认挂载 `./data/media` → 容器 `/data/media`。

---

## 2. 用 Docker Compose 部署（推荐）

### 2.1 补齐生产环境变量

编辑 `deploy/docker-compose.yml`，为 `api` 服务至少增加：

```yaml
environment:
  SPRING_PROFILES_ACTIVE: prod
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/xtv?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8
  SPRING_DATASOURCE_USERNAME: xtv
  SPRING_DATASOURCE_PASSWORD: "<强密码>"
  XTV_STORAGE_ROOT: /data/media
  XTV_PAYMENT_SIMULATE: "false"
  XTV_JWT_SECRET: "<至少32位随机串>"
  XTV_ADMIN_USERNAME: admin
  XTV_ADMIN_PASSWORD: "<强密码>"
  XTV_CORS_ORIGINS: "https://你的前台域名"
```

同步修改 `mysql` 服务的 `MYSQL_PASSWORD` / `MYSQL_ROOT_PASSWORD`，与上面数据源密码一致。

### 2.2 构建并启动

在仓库根目录：

```bash
cd deploy
docker compose build
docker compose up -d
docker compose ps
curl -s http://127.0.0.1:8080/actuator/health
# 期望 {"status":"UP"}
```

| 服务 | 宿主机端口 | 说明 |
|------|------------|------|
| `web` | 8088 → 80 | Nginx 静态站 + 反代 `/api/`、`/media/` |
| `api` | 8080 | Spring Boot |
| `mysql` | 3306 | 仅建议内网或防火墙限制 |

浏览器打开前台：`http://服务器IP:8088/`

### 2.2.1 怎么打开管理后台（必读）

1. 浏览器地址栏输入：**`http://服务器IP:8088/admin`**  
   - 本机 Docker：`http://127.0.0.1:8088/admin`  
   - 已配域名 HTTPS：`https://你的域名/admin`  
2. 页面会直接出现**后台登录框**（不是前台「我的 / 登录」页）。  
3. 用环境变量里的管理员账号登录（Compose 里配置的 `XTV_ADMIN_USERNAME` / `XTV_ADMIN_PASSWORD`）。  
4. 登录成功后左侧为后台菜单：影片、分类、收款方式、订单、平台配置等。  
5. 非管理员账号登录会提示无权限；前台普通用户入口是 `/login`，与后台无关。

> 站点顶部导航**没有**「管理后台」链接，必须手动访问 `/admin`。

### 2.3 反代 HTTPS（生产）

在前置 Nginx / Caddy / 云负载均衡终止 TLS，反代到 `web:80`。  
确保：

- 浏览器 Origin 与 `XTV_CORS_ORIGINS` 一致  
- WebSocket 非必需；普通 HTTPS 即可  
- 大文件上传：反代 `client_max_body_size` ≥ 片源上限（API 已放宽到约 2GB）

参考仓库内 `deploy/nginx/`（若有）或沿用 `apps/web/nginx.conf` 的路径规则：

- `/api/` → `api:8080`
- `/media/` → `api:8080`（签名 HLS / 封面）
- `/` → 前端 SPA

### 2.4 首次后台配置清单

登录 `/admin` 后按顺序：

1. **平台配置**：站点名、存储模式（本地 / 对象存储）、源站或 CDN 基址  
2. **收款方式**（见第 4 节）：填收款地址、网络（主网/Nile/BSC 测试网等）、Tron API Key  
3. **会员方案**：上架价格与天数  
4. **分类 / 标签 / 人物 / 短剧 / 影片**：录入内容并上传片源，等待转码完成后再发布  

本地 profile 会种子一批演示分类；**生产 `catalog-seed` 默认关闭**，内容需自行录入。

---

## 3. 不使用 Compose 的分步部署

### 3.1 MySQL

```bash
# 创建库与用户后，保证字符集 utf8mb4
```

### 3.2 API

```bash
cd apps/api
# 需本机 Java 17、Maven、FFmpeg
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/xtv?...'
export SPRING_DATASOURCE_USERNAME=xtv
export SPRING_DATASOURCE_PASSWORD=...
export XTV_STORAGE_ROOT=/var/xtv/media
export XTV_JWT_SECRET=...
export XTV_ADMIN_PASSWORD=...
export XTV_CORS_ORIGINS=https://www.example.com
mvn -DskipTests package
java -jar target/api-0.0.1-SNAPSHOT.jar
```

Flyway 会自动执行 `V1`…`V17` 迁移，**不要改已上线的迁移文件**。

### 3.3 Web

```bash
cd apps/web
npm ci
npm run build
# 将 dist/ 交由 Nginx；/api/ 与 /media/ 反代到 API
```

---

## 4. 支付如何接入（核心）

### 4.1 设计要点

1. **不接币安 Pay / Gate 等托管支付**（代码里相关渠道已关闭）。  
2. 用户把 **USDT** 转到平台地址；订单带有 **唯一尾数金额**（标价 + 随机 6 位小数），用来在链上辨认是哪一笔订单。  
3. 服务端定时任务 + 订单页轮询，调用链浏览器/节点 API 拉取收款地址近期入账，匹配金额与时间窗后 `markPaid`。  
4. 生产禁止 `payment-simulate`。

### 4.2 支持的渠道（有收款地址才会出现在前台）

| 方法 | 配置位置 | 核对数据源 |
|------|----------|------------|
| `USDT_TRC20` | 后台收款方式 → 波场地址 + 网络（MAINNET/NILE）+ API Key | TronGrid TRC20 流水 |
| `USDT_BEP20` | 币安链地址 + 网络 | BSC 节点/扫描 |
| `USDT_ERC20` 等 | 其它链收款地址表 `chain_receiver` | 对应 EVM / Solana / TON 客户端 |

### 4.3 后台怎么配（操作步骤）

1. 打开 **收款方式**  
2. **波场（TRC20）**  
   - 收款地址：以 `T` 开头的 Base58  
   - 网络：正式用 `MAINNET`；联调可用 `NILE`  
   - Tron API Key：TronGrid 控制台申请后粘贴（可降低限流）  
3. 若用 **BEP20**：填 `0x…` 地址，选主网或测试网  
4. 其它链：在「多链收款」里按链填写地址  
5. 点保存（确认地址无误再保存）  
6. 前台「会员」或 PPV 购买页应出现对应渠道按钮  

### 4.4 用户侧完整链路

```
用户登录
  → 选会员方案或 PPV 影片
  → POST /api/v1/orders  { type, planId|videoId, paymentMethod }
  → 返回订单：payAmount（唯一金额）、address、expiresAt（约 30 分钟）
  → 用户钱包按金额向 address 转 USDT（同网、同合约）
  → 钱包显示成功后，订单页仍会提示：平台还需链上核对（主网约再等约 1 分钟固化）
  → 前端每约 5 秒 POST /api/v1/orders/{id}/check
  → 后端 Job 约每 8 秒 billing.reconcile()
  → 匹配成功 → 订单 PAID → 写会员到期或 video_entitlement
  → 前端自动跳回播放/账号页
```

### 4.5 服务端对账规则（TRC20 为例）

实现：`BillingService.reconcileTron` / `check`。

对每笔 `PENDING` 且未过期订单：

1. 调 TronGrid：`/v1/accounts/{address}/transactions/trc20?only_confirmed=true&contract_address=USDT合约`  
2. 入账方向为收款地址、非自己转自己  
3. 金额（sun）= 订单 `payAmount` × 10^6  
4. 时间在「下单前 60 秒」到「过期后 120 秒」窗口内  
5. 主网：交易时间不晚于「链尖时间 − 约 60 秒」（降低重组风险）；Nile 测试网不做该等待  
6. `tx_hash` 未被其它订单占用  
7. 满足则 `markPaid`：写哈希、付款地址；会员延长期限或写入单集权益；记审计日志  

管理员仍可在后台对「待支付」订单点 **确认到账**（人工兜底）。

### 4.6 联调建议（测试网）

1. 后台网络选 **Nile**，填 Nile USDT 收款地址  
2. 用测试钱包向该地址转 **订单显示的精确金额**  
3. 留在订单页；提示条会说明「钱包成功 ≠ 已开通」  
4. 成功后状态变「已支付」，会员/影片解锁  

主网正式收款前：换主网地址、切 `MAINNET`、确认 API Key 与防火墙出网。

### 4.7 常见问题

| 现象 | 排查 |
|------|------|
| 前台没有支付渠道 | 后台未填收款地址或未保存 |
| 钱包成功但订单一直待支付 | 金额尾数不一致；网络主网/测试网不一致；合约不是 USDT；仍在主网固化窗口内；TronGrid 限流/失败（看 API 日志「对账失败」） |
| 生产起不来 | 未设 `XTV_JWT_SECRET` / 管理员密码，或开了模拟支付 |
| 重复入账 | 同 tx 有唯一约束；金额唯一分配降低串单 |

---

## 5. 媒体与转码

1. 后台「影片」上传片源 → 本地盘或对象存储  
2. `JobScheduler` 每 5 秒拉取转码任务，调用 FFmpeg 生成 HLS  
3. 播放走签名 URL：`/media/v/{exp}/{sig}/...`，匿名直链会被拦截  
4. 对象存储：在平台配置填 S3 Endpoint/桶/密钥；上传可走直传票据

确保 API 镜像含 FFmpeg（官方 `apps/api/Dockerfile` 已安装）。

---

## 6. 运维

### 6.1 健康检查

```bash
curl -s http://127.0.0.1:8080/actuator/health
```

K8s / Compose 可用该地址做 liveness（prod 已开 probes 配置项）。

### 6.2 备份

```bash
# 仓库根目录
./deploy/backup.sh
# 输出目录默认 data/backups/<时间戳>/（库 dump + media）
```

恢复：停 API → 还原 MySQL 或 H2 文件 → 还原 `media` → 再启动。

### 6.3 日志

```bash
cd deploy && docker compose logs -f api
```

关注：`波场对账失败`、`对账失败`、转码 `errorMessage`、限流 429。

### 6.4 限流（摘要）

登录 / 注册 / 下单 / `orders/*/check` / 投诉 / 播放上报 均有 IP 窗口限流（见 `RateLimitFilter`）。订单页自动查询已按约 5 秒间隔设计，勿对 check 接口做高频压测。

---

## 7. 本地开发（非生产）

```bash
# API（8080）
cd apps/api
# profile=local → 文件型 H2：仓库 data/xtv.mv.db，catalog-seed=true
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Web（Vite 默认 5173）
cd apps/web
npm run dev -- --host 127.0.0.1
```

### 7.1 本地怎么打开管理后台

| 项 | 地址 / 账号 |
|----|-------------|
| 前台 | http://127.0.0.1:5173/ |
| **管理后台** | **http://127.0.0.1:5173/admin** |
| 账号 / 密码 | `admin` / `admin123`（仅本地，勿用于生产） |

步骤：浏览器打开上面的 `/admin` → 输入管理员账号 → 进入左侧菜单。  
同样没有导航入口，必须自己敲 URL。

清空本地全部业务数据：

```bash
# 停掉占用 8080 的 API 后
rm -f data/xtv.mv.db data/xtv.trace.db data/xtv.lock.db
rm -rf data/media/*
# 再启动 local profile；会重建库、管理员，并按需种子演示分类
```

---

## 8. 上线检查清单

- [ ] `prod` profile + 强 JWT + 强管理员密码  
- [ ] `payment-simulate=false`  
- [ ] CORS 仅为真实前台 Origin  
- [ ] MySQL 强密码、仅内网可达  
- [ ] 收款地址与网络正确；小额主网或 Nile 实测一笔完整下单→转账→开通  
- [ ] HTTPS、备份脚本、磁盘空间（片源+HLS）  
- [ ] FFmpeg 可用；上传→转码→发布→播放闭环  
- [ ] 法律页 / 投诉入口可用  

---

## 9. 架构能力边界（实施预期）

**已具备、可支撑中小企业级业务：** 分层单体 API、Flyway、JWT 角色、生产密钥门禁、限流、审计日志、签名媒体、多链 USDT 轮询入账、Docker 交付、备份脚本。

**上更大流量 / 强合规前建议加强：** 独立只读库与连接池监控、集中日志与告警、支付对账幂等与人工对账台账、多实例时用分布式锁跑 reconcile、对象存储+CDN 标配、WAF、密钥托管（KMS）、更细的备份恢复演练。

详细评分见项目内架构评审 Canvas（对话侧可打开）。
