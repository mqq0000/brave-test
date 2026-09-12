package com.bravetest.service;

import com.bravetest.common.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 服务：文本对象上传 + 临时签名 URL（信息集借阅防盗链）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    /**
     * 外部可达地址（如 http://localhost:9000）。签名URL中的内部 endpoint 会被替换为该地址，
     * 避免容器内主机名（如 http://minio:9000）对浏览器不可达。留空则不替换。
     */
    @Value("${minio.public-endpoint:}")
    private String publicEndpoint;

    /**
     * 上传文本内容（信息集封装时调用）。失败仅记日志，由调用方回退数据库存储
     */
    public boolean putText(String objectKey, String content) {
        try {
            ensureBucket();
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType("text/plain; charset=utf-8")
                    .build());
            return true;
        } catch (Exception e) {
            log.warn("MinIO 上传失败（将回退数据库存储）: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 生成临时签名 GET URL，expirySeconds 后自动失效
     */
    public String presignedGetUrl(String objectKey, int expirySeconds) {
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build());
            if (publicEndpoint != null && !publicEndpoint.isBlank()) {
                java.net.URI uri = java.net.URI.create(url);
                String hostPart = uri.getScheme() + "://" + uri.getRawAuthority();
                String publicBase = publicEndpoint.replaceAll("/$", "");
                if (url.startsWith(hostPart) && !hostPart.equals(publicBase)) {
                    url = publicBase + url.substring(hostPart.length());
                }
            }
            return url;
        } catch (Exception e) {
            log.error("MinIO 签名URL生成失败: {}", e.getMessage());
            throw new BusinessException("静态资源服务暂不可用，请稍后再试");
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
