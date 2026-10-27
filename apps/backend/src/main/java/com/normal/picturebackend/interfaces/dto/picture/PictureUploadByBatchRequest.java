package com.normal.picturebackend.interfaces.dto.picture;

import lombok.Data;

/**
 * 图片批量上传请求
 */
@Data
public class PictureUploadByBatchRequest {

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 搜索数量
     */
    private Integer number = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;

}
