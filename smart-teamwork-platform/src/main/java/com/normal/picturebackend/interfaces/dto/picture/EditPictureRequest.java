package com.normal.picturebackend.interfaces.dto.picture;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 编辑图片请求
 */
@Data
public class EditPictureRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -2040000107946146853L;
    /**
     * id
     */
    @NotEmpty(message = "")
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;
}
