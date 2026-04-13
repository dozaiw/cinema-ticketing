package com.cinema.auth.util;

import com.cinema.auth.config.CosConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * COS 文件上传工具类
 * 支持海报、头像、预告片等多种文件类型上传
 *
 * @author makejava
 * @since 2026-02-15
 */
@Slf4j
@Component
public class CosUtil {

    @Autowired
    private COSClient cosClient;

    @Autowired
    private CosConfig cosConfig;

    // 支持的图片扩展名
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");

    // 支持的视频扩展名
    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList("mp4", "mov", "avi");

    // 哪些 fileType 属于图片类型
    private static final List<String> IMAGE_FILE_TYPES = Arrays.asList(
            "poster",           // 电影海报
            "avatar",           // 通用头像
            "actor/avatar",     // 演员头像
            "user/avatar",      // 用户头像
            "staff/avatar",      // 员工头像
            "qrCode",
            "advertisement"
    );

    /**
     * 上传文件到COS（返回CDN加速URL）
     *
     * @param file     待上传的文件
     * @param fileType 文件类型标识（如 "poster", "avatar", "trailer" 等）
     * @return CDN 加速后的文件URL，上传失败返回 null
     * @throws RuntimeException 文件校验失败或上传异常时抛出
     */
    public String uploadFile(MultipartFile file, String fileType) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            // 1. 校验文件大小
            long maxSize = isImageType(fileType) ?
                    cosConfig.getMaxPosterSize() * 1024 * 1024 :
                    cosConfig.getMaxTrailerSize() * 1024 * 1024;

            if (file.getSize() > maxSize) {
                throw new RuntimeException(
                        String.format("%s文件大小超过限制（最大%.1fMB）",
                                isImageType(fileType) ? "图片" : "视频",
                                maxSize / 1024.0 / 1024.0));
            }

            // 2. 校验文件扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();

            if (isImageType(fileType)) {
                // 图片类型校验
                if (!IMAGE_EXTENSIONS.contains(extension)) {
                    throw new RuntimeException("图片文件仅支持: " + String.join("/", IMAGE_EXTENSIONS));
                }
            } else {
                // 视频类型校验
                if (!VIDEO_EXTENSIONS.contains(extension)) {
                    throw new RuntimeException("视频文件仅支持: " + String.join("/", VIDEO_EXTENSIONS));
                }
            }

            // 3. 生成唯一文件名
            String uniqueFilename = generateUniqueFilename(fileType, extension);

            // 4. 设置对象元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            // 5. 上传到COS
            try (InputStream inputStream = file.getInputStream()) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        cosConfig.getBucketName(),
                        uniqueFilename,
                        inputStream,
                        metadata
                );
                cosClient.putObject(putObjectRequest);
            }

            // 6. 返回CDN加速URL
            String cdnUrl = cosConfig.getDomain() + "/" + uniqueFilename;
            log.info("✅ [{}] 上传成功: {}", fileType, cdnUrl);
            return cdnUrl;

        } catch (CosServiceException e) {
            log.error("❌ COS服务异常 [{}]: {}", fileType, e.getMessage());
            throw new RuntimeException("文件上传失败（COS服务）: " + e.getErrorMessage());
        } catch (CosClientException | IOException e) {
            log.error("❌ COS客户端异常 [{}]", fileType, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除COS中的文件
     *
     * @param fileUrl 文件的CDN URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // 从URL提取COS中的key（去掉域名前缀）
            String key = fileUrl.replace(cosConfig.getDomain() + "/", "");

            cosClient.deleteObject(cosConfig.getBucketName(), key);
            log.info("✅ 文件删除成功: {}", fileUrl);

        } catch (Exception e) {
            log.error("❌ 文件删除失败: {}", fileUrl, e);
            // 删除失败不抛异常，避免影响主业务流程
        }
    }

    /**
     * 判断指定 fileType 是否为图片类型
     *
     * @param fileType 文件类型标识
     * @return true-图片类型，false-视频类型
     */
    private boolean isImageType(String fileType) {
        if (fileType == null) {
            return false;
        }
        return IMAGE_FILE_TYPES.contains(fileType.toLowerCase());
    }

    /**
     * 生成唯一的文件存储路径
     *
     * @param fileType  文件类型（作为文件夹前缀）
     * @param extension 文件扩展名
     * @return COS中的唯一key，格式: {type}/{timestamp}_{uuid}.{ext}
     */
    private String generateUniqueFilename(String fileType, String extension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomStr = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 避免 fileType 为空或包含斜杠导致路径混乱
        String safeType = (fileType == null || fileType.isEmpty()) ? "upload" : fileType.replace("/", "_");

        return String.format("%s/%s_%s.%s", safeType, timestamp, randomStr, extension);
    }


    public String uploadFileFromBytes(byte[] bytes, String fileName, String fileType) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            String extension = FilenameUtils.getExtension(fileName).toLowerCase();

            // 扩展名校验
            if (isImageType(fileType) && !IMAGE_EXTENSIONS.contains(extension)) {
                throw new RuntimeException("图片文件仅支持: " + String.join("/", IMAGE_EXTENSIONS));
            }

            // 生成唯一COS路径: qrCode/1739520000000_a3f9b2.png
            String uniqueFilename = generateUniqueFilename(fileType, extension);

            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType("image/" + ("jpg".equals(extension) ? "jpeg" : extension));

            // 内存直传COS
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        cosConfig.getBucketName(),
                        uniqueFilename,
                        inputStream,
                        metadata
                );
                cosClient.putObject(putObjectRequest);
            }

            // 返回CDN URL
            String cdnUrl = cosConfig.getDomain() + "/" + uniqueFilename;
            log.info("✅ [{}] 内存直传COS成功: {}", fileType, cdnUrl);
            return cdnUrl;

        } catch (CosServiceException e) {
            log.error("❌ COS服务异常 [{}]: {}", fileType, e.getMessage());
            throw new RuntimeException("上传失败: " + e.getErrorMessage());
        } catch (CosClientException | IOException e) {
            log.error("❌ COS客户端异常 [{}]", fileType, e);
            throw new RuntimeException("上传失败: " + e.getMessage());
        }
    }
}