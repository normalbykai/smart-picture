package com.normal.picturebackend.interfaces.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间分析请求参数
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 是否查询公共图库 （公共图片）
     */
    private boolean queryPublic;

    /**
     * 全空间分析 （私有+公共）
     */
    private boolean queryAll;

    private static final long serialVersionUID = 1L;
}
