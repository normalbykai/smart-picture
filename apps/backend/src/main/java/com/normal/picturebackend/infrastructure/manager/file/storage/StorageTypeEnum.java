package com.normal.picturebackend.infrastructure.manager.file.storage;

import cn.hutool.core.util.StrUtil;

/**
 * 对象存储类型
 */
public enum StorageTypeEnum {

    /**
     * 腾讯云对象存储
     */
    COS("cos", "腾讯云 COS"),

    /**
     * 本地 MinIO 对象存储
     */
    MINIO("minio", "本地 MinIO");

    private final String value;

    private final String label;

    StorageTypeEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 根据配置值解析存储类型（忽略大小写）
     *
     * @param value 配置值，如 cos / minio
     * @return 存储类型，无法识别时返回 null
     */
    public static StorageTypeEnum from(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (StorageTypeEnum type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return null;
    }
}
