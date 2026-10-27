package com.normal.picturebackend.interfaces.dto.picture;

import lombok.Data;

import java.util.List;

/**
 * 图片分类 和 标签
 */
@Data
public class PictureTagCategory {

    // 标签列表
    private List<String> tagList;

    // 分类列表
    private List<String> categoryList;
}
