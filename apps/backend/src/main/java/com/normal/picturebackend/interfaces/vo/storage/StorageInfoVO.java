package com.normal.picturebackend.interfaces.vo.storage;

import lombok.Builder;
import lombok.Data;

/**
 * 单个对象存储信息
 */
@Data
@Builder
public class StorageInfoVO {

    /**
     * 存储类型：cos / minio
     */
    private String type;

    /**
     * 存储名称
     */
    private String label;

    /**
     * 配置是否完整
     */
    private boolean configured;

    /**
     * 是否为当前使用的存储
     */
    private boolean current;

    /**
     * 桶名
     */
    private String bucket;

    /**
     * 访问地址
     */
    private String endpoint;
}
