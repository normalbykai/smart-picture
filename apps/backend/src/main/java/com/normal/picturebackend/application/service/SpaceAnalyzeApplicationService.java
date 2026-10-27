package com.normal.picturebackend.application.service;

import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.interfaces.dto.space.*;
import com.normal.picturebackend.interfaces.vo.space.*;

import java.util.List;

/**
 * 空间分析服务层
 */
public interface SpaceAnalyzeApplicationService {

    /**
     * 获取空间分析使用结果
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyzeResult(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

    /**
     * 获取空间图片分类分析
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyzeResult(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 标签分析
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyzeResult(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 图片大小分析
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyzeResult(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 用户上传行为分析
     */
    List<SpaceUserAnalyzeResponse>  getSpaceUserAnalyzeResult(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 空间使用排行榜 TOP10
     */
    List<Space>  getSpaceRankAnalyzeResult(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);


}
