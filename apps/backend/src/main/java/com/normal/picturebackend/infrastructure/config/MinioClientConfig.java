package com.normal.picturebackend.infrastructure.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置
 */
@Configuration
public class MinioClientConfig {

    @Bean
    public MinioClient minioClient(StorageProperties storageProperties) {
        StorageProperties.Minio minio = storageProperties.getMinio();
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }
}
