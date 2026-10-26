package com.normal.picturebackend.infrastucture.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.interfaces.dto.space.*;
import com.normal.picturebackend.interfaces.vo.space.*;

import java.util.List;

/**
* @author norma
* @description 针对表【picture(图片)】的数据库操作Mapper
* @createDate 2025-04-19 16:54:15
* @Entity com.normal.picturebackend.domain.picture.entity.Picture
*/
public interface PictureMapper extends BaseMapper<Picture> {



    /**
     * 获取空间已使用大小 前N名空间信息
     * @param spaceRankAnalyzeRequest
     */
    List<Space> getRankTopN(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest);

    /**
     * 获取空间分类相关分析
     *
     * @param spaceCategoryAnalyzeRequest
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest);


    /**
     * 获取标签分析
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest);


    /**
     * 获取空间文件大小分析
     *
     * @param spaceSizeAnalyzeRequest
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest);


    /**
     * 获取全部空间图片分析结果
     * @return
     */
    SpaceUsageAnalyzeResponse getAllUsageAnalyze();

    /**
     * 获取公共空间图片分析结果
     *
     * @return
     */
    SpaceUsageAnalyzeResponse getPublicUsageAnalyze();

    /**
     *
     * @param spaceUserAnalyzeRequest
     */
    List<SpaceUserAnalyzeResponse> getSPaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest);
}




