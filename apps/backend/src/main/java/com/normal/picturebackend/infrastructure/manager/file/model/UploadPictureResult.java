package com.normal.picturebackend.infrastructure.manager.file.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 图片上传后结果信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadPictureResult {  
  
    /**  
     * 图片地址  
     */  
    private String url;  
  
    /**  
     * 图片名称  
     */  
    private String picName;

    /**
     * 缩略图url地址
     */
    private String thumbnailUrl;

    /**
     * 原图地址
     */
    private String originUrl;

    /**  
     * 文件体积  
     */  
    private Long picSize;  
  
    /**  
     * 图片宽度  
     */  
    private int picWidth;  
  
    /**  
     * 图片高度  
     */  
    private int picHeight;  
  
    /**  
     * 图片宽高比  
     */  
    private Double picScale;  
  
    /**  
     * 图片格式  
     */  
    private String picFormat;

    /**
     * 搜索文本
     */
    private String searchText;

    /**
     * 主色调
     */
    private String mainColor;

}

