package com.normal.picturebackend.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 以图搜图请求
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {

    private static final long serialVersionUID = -7322115227634896064L;

    /**
     * 图片ID
     */
    private Long pictureId;



}
