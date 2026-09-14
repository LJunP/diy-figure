package com.diyfigure.integration.oss;

import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 本地磁盘文件存储(OSS 未配置时的降级方案)
 *
 * 为什么需要它:
 * OSS 密钥未配置时,旧实现把上传的参考图直接丢弃、固定返回一张 placehold.co 的示意图。
 * 结果是「用户上传了参考图,但对话里看到的是别人的图」——Demo 流程自相矛盾,
 * 也无法验证上传链路本身是否正常。
 *
 * 现在改为落本地磁盘,由 {@code WebMvcConfig} 把 {@code /uploads/**} 映射到该目录,
 * 上传接口返回可访问的真实 URL。生产环境配好 OSS 后这段逻辑不会被走到。
 *
 * 存储路径:{rootDir}/{directory}/{yyyy-MM-dd}/{uuid}.{ext}
 */
@Slf4j
@Service
public class LocalFileStorage {

    /** 落盘根目录。相对路径按后端进程工作目录解析 */
    @Value("${diy.local-storage.dir:./data/uploads}")
    private String rootDir;

    /**
     * 对外访问前缀。
     *
     * 留空(默认)时按当前请求推导 —— 这样返回的 URL 天然带上真实的 host、端口和 context-path,
     * 不会出现「服务跑在 8088,返回的却是 8080 的链接」这种配置漂移。
     *
     * 只有部署在反向代理后面、请求头里的 host 不是最终对外域名时,才需要显式配置它。
     */
    @Value("${diy.public-base-url:}")
    private String publicBaseUrl;

    /** 兜底前缀:既没有显式配置、也拿不到请求上下文时才会用到 */
    private static final String FALLBACK_BASE_URL = "http://localhost:8080/api";

    /** 是否启用本地存储(生产环境可用 DIY_LOCAL_STORAGE_ENABLED=false 关闭) */
    @Value("${diy.local-storage.enabled:true}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    /** 落盘根目录的绝对路径,供静态资源映射使用 */
    public String getAbsoluteRootDir() {
        return Paths.get(rootDir).toAbsolutePath().normalize().toString();
    }

    /**
     * 保存上传文件,返回可访问 URL
     *
     * @param file      前端上传的文件
     * @param directory 逻辑目录(references / canvases 等)
     * @return {baseUrl}/uploads/{directory}/{date}/{uuid}.{ext}
     */
    public String store(MultipartFile file, String directory) {
        return store(file, directory, null);
    }

    /**
     * 保存上传文件,返回可访问 URL
     *
     * @param requestBaseUrl 调用方从当前请求推导出的前缀(如 http://host:port/api),可为 null
     */
    public String store(MultipartFile file, String directory, String requestBaseUrl) {
        if (!enabled) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED,
                    "OSS 未配置且本地存储已关闭,无法上传文件");
        }

        String ext = extensionOf(file.getOriginalFilename());
        String datePath = LocalDate.now().toString();
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String relativePath = directory + "/" + datePath + "/" + fileName;

        Path target = Paths.get(rootDir, relativePath).toAbsolutePath().normalize();
        Path root = Paths.get(rootDir).toAbsolutePath().normalize();

        // 防目录穿越:directory 来自服务端常量,这里再兜一道,避免将来被改成用户可控
        if (!target.startsWith(root)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "非法的上传路径");
        }

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            String url = resolveBaseUrl(requestBaseUrl) + "/uploads/" + relativePath;
            log.info("文件已保存到本地磁盘: {} → {}", target, url);
            return url;
        } catch (IOException e) {
            log.error("本地文件保存失败: {}", target, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "文件保存失败: " + e.getMessage());
        }
    }

    /** 优先级:显式配置 > 当前请求 > 兜底常量 */
    private String resolveBaseUrl(String requestBaseUrl) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return stripTrailingSlash(publicBaseUrl);
        }
        if (requestBaseUrl != null && !requestBaseUrl.isBlank()) {
            return stripTrailingSlash(requestBaseUrl);
        }
        return FALLBACK_BASE_URL;
    }

    private String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        // 只保留字母数字的短后缀,避免奇怪文件名带来的路径问题
        String ext = originalFilename.substring(dot + 1);
        return ext.matches("[A-Za-z0-9]{1,8}") ? "." + ext.toLowerCase() : "";
    }
}
