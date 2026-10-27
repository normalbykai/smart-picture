package com.normal.picturebackend.interfaces.dto.storage;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 切换对象存储请求
 */
@Data
public class StorageSwitchRequest {

    /**
     * 存储类型：cos / minio
     */
    @NotBlank(message = "存储类型不能为空")
    private String type;
}
