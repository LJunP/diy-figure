package com.diyfigure.integration.oss;

import com.aliyun.oss.OSS;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.config.OssConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传服务
 *
 * 负责将用户上传的参考图、AI 生成的概念图、3D 模型文件上传到 OSS
 * 文件路径规则:{directory}/{date}/{uuid}.{ext}
 *
 * 注意:如果 OSS 密钥未配置(占位值),上传操作会抛出异常
 * 开发测试阶段 AI 服务可能无法调用,代码中做了降级处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    private final OssConfig ossConfig;

    /** 允许上传的图片格式 */
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    /** 允许上传的 3D 模型格式 */
    private static final List<String> ALLOWED_MODEL_TYPES = Arrays.asList(
            "model/gltf-binary", "application/octet-stream"
    );

    /**
     * 上传图片到 OSS
     *
     * @param file 前端上传的文件
     * @param directory OSS 目录(canvases/references/generated 等)
     * @return 文件的完整访问 URL
     */
    public String uploadImage(MultipartFile file, String directory) {
        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED,
                    "不支持的图片格式,仅支持 JPEG/PNG/GIF/WebP");
        }

        return uploadFile(file, directory);
    }

    /**
     * 上传文件到 OSS(通用方法)
     *
     * @param file 文件
     * @param directory OSS 目录
     * @return 文件访问 URL
     */
    public String uploadFile(MultipartFile file, String directory) {
        OSS ossClient = ossConfig.getOssClient();
        if (ossClient == null) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED,
                    "OSS 未配置,请设置环境变量 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET");
        }

        // 生成文件路径: {directory}/{date}/{uuid}.{ext}
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String datePath = LocalDate.now().toString();
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String objectKey = directory + "/" + datePath + "/" + fileName;

        try {
            ossClient.putObject(ossConfig.getBucketName(), objectKey, file.getInputStream());
            String url = ossConfig.getDomain() + "/" + objectKey;
            log.info("文件上传成功: {}", url);
            return url;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 将 URL 字符串上传到 OSS(用于 AI 生成的图片,先下载再上传)
     * 开发阶段如果 OSS 未配置,返回原始 URL 作为降级方案
     *
     * @param sourceUrl AI 服务返回的图片 URL
     * @param directory OSS 目录
     * @return OSS 上的文件 URL(或原始 URL 如果 OSS 未配置)
     */
    public String transferFromUrl(String sourceUrl, String directory) {
        OSS ossClient = ossConfig.getOssClient();
        if (ossClient == null) {
            // 降级:直接返回原始 URL
            log.warn("OSS 未配置,直接返回原始 URL: {}", sourceUrl);
            return sourceUrl;
        }

        try {
            // 下载文件
            java.net.URL url = new java.net.URL(sourceUrl);
            String path = url.getPath();
            String ext = path.contains(".") ? path.substring(path.lastIndexOf(".")) : ".png";
            String datePath = LocalDate.now().toString();
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            String objectKey = directory + "/" + datePath + "/" + fileName;

            try (var inputStream = url.openStream()) {
                ossClient.putObject(ossConfig.getBucketName(), objectKey, inputStream);
            }

            String ossUrl = ossConfig.getDomain() + "/" + objectKey;
            log.info("文件转存成功: {} → {}", sourceUrl, ossUrl);
            return ossUrl;
        } catch (Exception e) {
            log.error("文件转存失败,返回原始 URL: {}", sourceUrl, e);
            return sourceUrl;
        }
    }

    /**
     * 删除 OSS 上的文件
     *
     * @param fileUrl 文件完整 URL
     */
    public void deleteFile(String fileUrl) {
        OSS ossClient = ossConfig.getOssClient();
        if (ossClient == null) {
            return;
        }

        try {
            // 从 URL 提取 objectKey
            String objectKey = fileUrl.replace(ossConfig.getDomain() + "/", "");
            ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
            log.info("文件删除成功: {}", fileUrl);
        } catch (Exception e) {
            log.warn("文件删除失败: {}", fileUrl, e);
        }
    }
}
