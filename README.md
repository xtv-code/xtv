# XTV

点播站点。后端是 Spring Boot 3.5 + JPA，测试用 H2，生产用 MySQL。转码由定时任务调用本机 FFmpeg。收藏存在浏览器本地。会员和单集购买首期使用 TRC20 USDT，开发环境可以模拟到账。

## 本地直接跑

本机已安装 JDK 17 时，可以用 H2 文件库，不必先起 MySQL：

```bash
cd apps/api
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

另一个终端：

```bash
cd apps/web
npm install
npm run dev
```

打开 http://localhost:5173 。管理后台在 `/admin`，默认账号 `admin` / `admin123`。

## MySQL

```bash
docker compose -f deploy/docker-compose.yml up mysql
cd apps/api && mvn spring-boot:run
```

`dev` profile 连接 `localhost:3306` 的库 `xtv`，用户和密码都是 `xtv`。

## 配置

- `XTV_USDT_ADDRESS`：TRC20 收款地址
- `XTV_TRON_API_KEY`：TronGrid Key
- `XTV_S3_ENDPOINT`、`XTV_S3_BUCKET`、`XTV_S3_ACCESS_KEY`、`XTV_S3_SECRET_KEY`：切到对象存储前要配好
- `XTV_JWT_SECRET`：生产环境的登录密钥
