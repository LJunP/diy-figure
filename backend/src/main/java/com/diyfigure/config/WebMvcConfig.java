package com.diyfigure.config;

import com.diyfigure.auth.AdminInterceptor;
import com.diyfigure.auth.JwtInterceptor;
import com.diyfigure.integration.oss.LocalFileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 1. 配置 CORS(跨域资源共享),允许前端 Vue 开发服务器访问
 * 2. 注册 JWT 拦截器,保护需要认证的接口
 * 3. OSS 未配置时,把 /uploads/** 映射到本地磁盘(见 LocalFileStorage)
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    private final AdminInterceptor adminInterceptor;

    private final LocalFileStorage localFileStorage;

    /**
     * 允许的前端来源。开发环境可用 *,生产环境必须通过 CORS_ALLOWED_ORIGINS 指定具体域名
     */
    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地存储的静态资源。OSS 配置后这条映射不会被访问,但保留不影响
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + localFileStorage.getAbsoluteRootDir() + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                // 白名单:不需要认证的接口
                .excludePathPatterns(
                        "/auth/register",
                        "/auth/login",
                        "/auth/verify-email",
                        "/auth/resend-verification",
                        "/auth/forgot-password",
                        "/auth/reset-password",
                        "/health",
                        "/error",
                        // 图片本身就是公开资源(URL 含随机 UUID),浏览器 <img> 不会带 Authorization 头
                        "/uploads/**",
                        // 支付平台服务端回调不带 JWT;安全性完全靠控制器内部验签
                        "/payments/callback/wechat",
                        "/payments/callback/alipay"
                );

        // 运营端接口:在认证之后再做一次角色校验,防止普通用户越权操作
        // 覆盖 /orders/admin/**、/production/admin/** 等所有 /{module}/admin/** 路径
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/**/admin/**");
    }
}
