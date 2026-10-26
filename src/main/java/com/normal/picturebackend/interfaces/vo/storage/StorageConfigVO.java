package com.normal.picturebackend.interfaces.vo.storage;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 对象存储配置视图
 */
@Data
@Builder
public class StorageConfigVO {

    /**
     * 当前存储类型
     */
    private String currentType;

    /**
     * 当前存储名称
     */
    private String currentLabel;

    /**
     * 全部存储
     */
    private List<StorageInfoVO> storages;
}
