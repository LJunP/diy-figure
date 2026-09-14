package com.diyfigure.integration.oss;

import com.diyfigure.auth.JwtInterceptor;
import com.diyfigure.common.response.ApiResponse;
import com.diyfigure.config.OssConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

/**
 * 文件上传控制器
 *
 * 用于用户上传参考图(在 AI 对话中作为 GPT-4o 的图片输入)
 * 文件上传到阿里云 OSS,返回访问 URL
 */
@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final OssService ossService;
    private final OssConfig ossConfig;
    private final LocalFileStorage localFileStorage;

    /**
     * 上传参考图
     * 前端使用 FormData 上传文件
     *
     * @param file 图片文件(JPEG/PNG/GIF/WebP)
     * @return { url: "https://oss.../xxx.png" }
     */
    @PostMapping("/upload-image")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file,
                                                        HttpServletRequest request) {
        // JWT 拦截器已校验登录状态,此处获取 userId 用于日志
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);

        // 校验图片格式(与 OSS 通道保持一致,避免两条路径行为不一致)
        ossService.validateImageType(file);

        // OSS 未配置时落本地磁盘,返回可访问的 URL。
        // 不能返回固定占位图:那样用户上传的参考图会被丢弃,对话里看到的是别人的图。
        if (ossConfig.getOssClient() == null) {
            // 按当前请求推导前缀,避免服务实际端口与配置里的默认端口不一致时返回错误链接
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String url = localFileStorage.store(file, "references", baseUrl);
            log.info("OSS 未配置,参考图保存到本地: userId={}, url={}", userId, url);
            return ApiResponse.success(Map.of("url", url));
        }

        String url = ossService.uploadImage(file, "references");
        return ApiResponse.success(Map.of("url", url));
    }
}
