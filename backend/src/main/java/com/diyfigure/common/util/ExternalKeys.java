package com.diyfigure.common.util;

import java.util.Locale;

/**
 * 判断外部服务密钥是否为「可真实调用」的值。
 *
 * 占位串、空串、过短的值一律视为未配置,走模拟/降级分支。
 * 不要只匹配某一个字面量,否则空密钥会打到真实 HTTP 端点。
 */
public final class ExternalKeys {

    private ExternalKeys() {
    }

    public static boolean isConfigured(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 12) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("your-") || lower.contains("placeholder")
                || lower.contains("changeme") || lower.contains("please-change")
                || lower.contains("example")) {
            return false;
        }
        return true;
    }
}
