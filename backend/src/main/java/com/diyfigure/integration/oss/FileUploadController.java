package com.diyfigure.integration.oss;

import com.diyfigure.auth.JwtInterceptor;
import com.diyfigure.common.response.ApiResponse;
import com.diyfigure.config.OssConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传控制器
 *
 * 用于用户上传参考图(在 AI 对话中作为 GPT-4o 的图片输入)
 * 文件上传到阿里云 OSS,返回访问 URL
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final OssService ossService;
    private final OssConfig ossConfig;

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

        // Demo 模式:OSS 未配置时返回占位 URL
        if (ossConfig.getOssClient() == null) {
            String placeholderUrl = "https://placehold.co/400x400/409eff/ffffff/png?text=Reference+Image";
            return ApiResponse.success(Map.of("url", placeholderUrl));
        }

        String url = ossService.uploadImage(file, "references");
        return ApiResponse.success(Map.of("url", url));
    }
}
