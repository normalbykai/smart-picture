package com.normal.picturebackend.domain.space.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.interfaces.dto.space.SpaceAddRequest;
import com.normal.picturebackend.interfaces.dto.space.SpaceQueryRequest;
import com.normal.picturebackend.interfaces.vo.space.SpaceUsageAnalyzeResponse;
import com.normal.picturebackend.interfaces.vo.space.SpaceVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 空间服务层
 *
 * @author norma
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-05-04 14:17:22
 */
public interface SpaceDomainService extends IService<Space> {





    /**
     * 获取查询条件
     * @param spaceQueryRequest 查询条件
     * @return 查询包装类
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * - 1.校验空间是否存在
     * - 2.校验仅空间所属者或管理员可访问
     *
     *
     * @param loginUser
     * @param spaceId
     */
    Space checkSpaceAuth(User loginUser, Long spaceId);

    /**
     * 统计空间资源使用情况
     *
     * @param spaceId 空间id
     * @return 空间资源使用情况
     */
    SpaceUsageAnalyzeResponse countSpaceUsageAnalyze(Long spaceId);


}
