package com.diyfigure.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 阿里云 OSS 配置
 * 负责初始化 OSSClient 实例,供文件上传服务使用
 *
 * 注意:实际使用时需要通过环境变量注入 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET
 */
@Slf4j
@Getter
@Configuration
public class OssConfig {

    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.access-key-id}")
    private String accessKeyId;

    @Value("${oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${oss.bucket-name}")
    private String bucketName;

    @Value("${oss.domain}")
    private String domain;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        // 在密钥为占位值时不初始化客户端,避免启动报错
        if (accessKeyId.contains("your-access-key-id")) {
            log.warn("OSS 密钥未配置,跳过 OSS 客户端初始化。请设置环境变量 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET");
            return;
        }
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        log.info("阿里云 OSS 客户端初始化成功, endpoint={}", endpoint);
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("阿里云 OSS 客户端已关闭");
        }
    }
}
