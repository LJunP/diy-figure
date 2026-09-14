# 部署、故障定位与回滚

## 发布

1. 构建后端:`cd backend && mvn -B -DskipTests package`
2. 构建前端:`cd frontend && npm ci && npm run build`
3. 把 `frontend/dist` 放到 Nginx `root`
4. 用 `deploy/nginx.conf` 配反代,**SSE location 必须 `proxy_buffering off`**
5. 生产启动必须 `SPRING_PROFILES_ACTIVE=prod`,并注入 `JWT_SECRET`、`DB_*`、`CORS_ALLOWED_ORIGINS`

```bash
docker compose up -d --build
curl --noproxy '*' http://localhost:8080/api/health
```

本地演示请保持 `SPRING_PROFILES_ACTIVE=dev`(compose 默认),否则模拟支付被 prod 硬关。

## 备份与恢复

```bash
# 备份
mysqldump -uroot -p --single-transaction --routines --triggers diy_figure > backup-$(date +%F).sql

# 恢复(会覆盖目标库)
mysql -uroot -p diy_figure < backup-YYYY-MM-DD.sql
```

恢复后核对 `flyway_schema_history` 版本与当前代码 `db/migration` 一致。不一致时不要改已执行的 Vn 文件,只加新版本。

## 回滚

1. 停应用
2. 恢复上一份 `mysqldump`(若本次发布含迁移,必须先回库再回代码)
3. 部署上一版 jar / 前端 dist
4. `curl /api/health`,登录,打开一条订单详情

没有蓝绿集群时,回滚窗口内不要接真实支付回调。

## 故障定位

| 现象 | 先看 |
|---|---|
| 登录 401 | JWT 是否过期;提权后是否重新登录 |
| 运营接口 403 | 角色是否 ADMIN;拦截器是否命中 `/**/admin/**` |
| 业务提示但 HTTP 200 | 看响应体 `code`,不要只看 HTTP 状态 |
| SSE 一次吐完 | Nginx 是否缓冲了 `/api/canvases/*/chat` |
| 图片 502 | `DIY_PUBLIC_BASE_URL` 是否写死了错误端口 |
| 集成测试偶发 3D SUCCESS | 测试 profile 必须 `diy.scheduling.enabled=false` |
| 支付回调 401 | 路径应在 JWT 白名单;`WECHAT_PAY_API_KEY` 未配时必须失败关闭 |
| 上传失败(容器) | `/app/data/uploads` 是否可写 |

日志关键字:`订单状态转移`、`模拟支付`、`OSS 密钥未配置`、`邮件服务未配置`。
