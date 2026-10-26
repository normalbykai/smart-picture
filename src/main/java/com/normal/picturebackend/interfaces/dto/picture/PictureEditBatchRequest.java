package com.normal.picturebackend.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片编辑批量请求
 */

@Data
public class PictureEditBatchRequest implements Serializable {

    /**
     * 图片Id 列表
     */
    private List<Long> pictureIdList;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;


    /**
     * 命名规则
     */
    private String nameRule;

    // 序列化 1L
    private static final Long serialVersionUID = 1L;
}
