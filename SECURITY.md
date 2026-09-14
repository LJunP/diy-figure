# 安全说明

## 报告漏洞

请不要在公开 issue 里贴可利用细节。通过仓库维护者的私下渠道报告,并附:

- 影响的接口或页面
- 复现步骤
- 是否涉及越权、支付绕过、信息泄露

仓库目前没有赏金计划。

## 已知安全边界(不是漏洞,是范围)

- 支付仍为模拟。`POST /api/payments/{id}/simulate` 仅非 prod 且 `payment.mock-enabled=true`
- 微信/支付宝回调已加入 JWT 白名单,但必须配置 `WECHAT_PAY_API_KEY` / `ALIPAY_PAY_API_KEY` 才能验签入账;未配置时失败关闭
- 真实商户 SDK、证书、退款打款未接入
- `/uploads/**` 公开可读(文件名含 UUID)

## 生产检查

- `JWT_SECRET` ≥ 32 字节随机串,不要用仓库默认值
- `SPRING_PROFILES_ACTIVE=prod`
- `CORS_ALLOWED_ORIGINS` 写具体前端域名
- `PAYMENT_MOCK_ENABLED=false`(prod profile 已硬关)
- 不要开启 `ADMIN_INIT_ENABLED`
