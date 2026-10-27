package com.normal.picturebackend.infrastructure.ai;

import com.normal.picturebackend.infrastructure.config.AiServiceProperties;
import com.normal.picturebackend.infrastructure.config.StorageProperties;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** 仅为已知 MinIO 桶中的现有图片签发 GET 地址，不接受前端传入的下载 URL。 */
@Component
public class AiImageUrlSigner {
    private final MinioClient signingClient;
    private final StorageProperties storage;
    private final AiServiceProperties ai;

    public AiImageUrlSigner(MinioClient minioClient, StorageProperties storage, AiServiceProperties ai) {
        this.storage = storage;
        this.ai = ai;
        this.signingClient = StringUtils.hasText(ai.getMinioEndpoint())
                ? MinioClient.builder().endpoint(ai.getMinioEndpoint())
                    .credentials(storage.getMinio().getAccessKey(), storage.getMinio().getSecretKey()).build()
                : minioClient;
    }

    public String sign(String storedUrl) {
        String key = objectKey(storedUrl);
        try {
            return signingClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET).bucket(storage.getMinio().getBucket()).object(key)
                    .expiry(ai.getImageUrlExpirySeconds(), TimeUnit.SECONDS).build());
        } catch (Exception e) {
            // 不暴露存储地址、签名或底层异常内容。
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成图片临时访问地址失败");
        }
    }

    String objectKey(String storedUrl) {
        try {
            URI image = URI.create(storedUrl);
            if (image.getUserInfo() != null || image.getFragment() != null || image.getHost() == null) {
                throw new IllegalArgumentException();
            }
            for (String value : List.of(storage.getMinio().getEndpoint(),
                    storage.getMinio().getEffectivePublicUrl())) {
                URI base = URI.create(value);
                String prefix = base.getPath().replaceAll("/+$", "")
                        + "/" + storage.getMinio().getBucket() + "/";
                if (base.getScheme().equalsIgnoreCase(image.getScheme())
                        && base.getAuthority().equalsIgnoreCase(image.getAuthority())
                        && image.getPath().startsWith(prefix)) {
                    // URI.getPath() 只解码一次，保留对象键中的加号和字面百分号。
                    String key = image.getPath().substring(prefix.length());
                    if (!key.isBlank() && !key.contains("\\")
                            && java.util.Arrays.stream(key.split("/")).noneMatch(p -> p.equals("..") || p.equals("."))) {
                        return key;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // 不将非法或不匹配的 URL 回退到当前存储。
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "智能分析目前仅支持配置的 MinIO 桶内图片");
    }
}
