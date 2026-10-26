package com.normal.picturebackend.infrastucture.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对象存储配置
 * <p>
 * 通过 {@code storage.type} 在 cos / minio 之间切换，默认使用本地 MinIO。
 *
 * <pre>
 * storage:
 *   type: minio
 *   minio:
 *     endpoint: http://127.0.0.1:9000
 *     access-key: minioadmin
 *     secret-key: minioadmin
 *     bucket: smart-image
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * 当前使用的对象存储类型：cos / minio
     */
    private String type = "minio";

    /**
     * MinIO 配置
     */
    private Minio minio = new Minio();

    @Data
    public static class Minio {

        /**
         * MinIO 服务地址（后端 SDK 使用）
         */
        private String endpoint = "http://127.0.0.1:9000";

        /**
         * 图片对外访问地址，默认与 endpoint 一致。
         * 如果 MinIO 部署在 Nginx / CDN 后面，可配置为对外域名。
         * 最终访问地址为：publicUrl/bucket/objectKey
         */
        private String publicUrl;

        /**
         * 访问密钥
         */
        private String accessKey = "minioadmin";

        /**
         * 密钥
         */
        private String secretKey = "minioadmin";

        /**
         * 桶名
         */
        private String bucket = "smart-image";

        /**
         * 桶不存在时是否自动创建
         */
        private boolean autoCreateBucket = true;

        /**
         * 是否将桶设置为公共读（图片需要通过 URL 直接访问/展示）
         *
         * <p>默认关闭：公共读意味着任何拿到 URL 的人都能访问图片。
         * 本地开发可在 application-local.yml 中显式开启，生产环境建议配合 CDN / 签名 URL。</p>
         */
        private boolean publicRead = false;

        /**
         * 缩略图最大边长（px）
         */
        private int thumbnailMaxSize = 512;

        /**
         * 超过该大小才生成缩略图（字节），与 COS 的 20KB 规则保持一致
         */
        private long thumbnailMinBytes = 20 * 1024L;

        /**
         * 非公共读模式下，预签名 URL 的有效期（秒），最大 7 天
         */
        private int presignedExpirySeconds = 7 * 24 * 60 * 60;

        /**
         * 获取实际对外访问地址
         */
        public String getEffectivePublicUrl() {
            String url = StrUtil.blankToDefault(publicUrl, endpoint);
            return StrUtil.removeSuffix(StrUtil.nullToEmpty(url), "/");
        }
    }
}
