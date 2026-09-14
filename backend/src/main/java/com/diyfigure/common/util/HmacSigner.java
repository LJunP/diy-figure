package com.diyfigure.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HMAC-SHA256 签名。支付回调在接入官方 SDK 之前用这一套做失败关闭的验签。
 */
public final class HmacSigner {

    private HmacSigner() {
    }

    public static String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    /**
     * 按 key 排序后拼成 k=v&k=v,空值跳过,sign 本身不参与。
     */
    public static String canonicalQuery(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .filter(e -> !"sign".equalsIgnoreCase(e.getKey()))
                .filter(e -> !e.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    public static boolean matches(String secret, String payload, String providedSign) {
        if (secret == null || providedSign == null || providedSign.isBlank()) {
            return false;
        }
        String expected = sign(secret, payload);
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = providedSign.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return a.length == b.length && MessageDigest.isEqual(a, b);
    }
}
