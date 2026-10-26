package com.normal.picturebackend.interfaces.dto.picture;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureUploadRequest implements Serializable {
  
    /**  
     * 图片 id（用于修改）  
     */
    private Long id;

    /**
     * 图片地址
     */
    private String fileUrl;

    /**
     * 图片名字
     */
    private String picName;

    /**
     * 空间ID
     */
    private Long spaceId;

    @Serial
    private static final long serialVersionUID = 1L;  
}

