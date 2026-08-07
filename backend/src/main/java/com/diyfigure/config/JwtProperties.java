package com.diyfigure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * JWT 配置属性
 * 从 application.yml 中读取 jwt.* 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** JWT 签名密钥 */
    private String secret;

    /** token 有效期(毫秒) */
    private long expiration;

    /** 请求头名称 */
    private String header;

    /** token 前缀 */
    private String prefix;
}
