# 贡献指南

## 本地开发

见 README「本地开发」。后端常用端口 8088,前端 `VITE_BACKEND_TARGET=http://localhost:8088 npm run dev`。

```bash
cd backend && mvn -o test -DTEST_DB_PASSWORD=你的密码
cd frontend && npm run test && npm run build
```

测试库 `diy_figure_it` 必须与开发库分开。集成测试会 `flyway.clean()`。

## 提交约定

- 改 JPA 实体必须同时加 Flyway 新版本,不要改已执行的 `V1`–`Vn`
- 订单状态转移只走 `OrderStateMachineService`,不要直接 `setStatus`
- 终审 `REVIEWING → QUOTED` 禁止写默认通过
- 业务错误返回 HTTP 200 + body `code`;拦截器失败才是真 401/403
- 不要提交 `.env`、密钥、真实地址或数据库转储

## 验证

改完相关模块后至少跑受影响的测试,并同步 `README.md` / `02-产品需求说明书.md` / `03-技术设计说明书.md` 里的事实性描述。
