package com.normal.picturebackend.infrastructure.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceProperties {
    private String baseUrl = "http://127.0.0.1:8334";
    // 不生成 toString，避免共享密钥出现在配置日志中。
    private String apiKey = "";
    @Min(1)
    private int connectTimeoutMs = 3000;
    @Min(1)
    private int readTimeoutMs = 100000;
    /** Python 可访问的 MinIO 签名源地址；空值使用 storage.minio.endpoint。 */
    private String minioEndpoint = "";
    @Min(30)
    @Max(600)
    private int imageUrlExpirySeconds = 120;

    @AssertTrue(message = "AI base URL must be an HTTP(S) origin without credentials, query or fragment")
    public boolean isValidBaseUrl() {
        try {
            URI uri = URI.create(baseUrl);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null
                    && uri.getQuery() == null && uri.getFragment() == null
                    && (uri.getPath().isEmpty() || "/".equals(uri.getPath()));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
