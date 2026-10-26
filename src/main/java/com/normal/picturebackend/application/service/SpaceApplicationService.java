package com.normal.picturebackend.application.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.normal.picturebackend.interfaces.dto.space.SpaceAddRequest;
import com.normal.picturebackend.interfaces.dto.space.SpaceQueryRequest;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.interfaces.vo.space.SpaceVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 空间服务层
 *
 * @author norma
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-05-04 14:17:22
 */
public interface SpaceApplicationService extends IService<Space> {

    /**
     * 转换space对象为VO
     *
     * @param space   空间对象
     * @param request http请求
     * @return 空间视图对象
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 将分页对象 转换为VO
     * @param spacePage 分页的图片对象
     * @param request   用户请求参数
     * @return 分页VO对象
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询条件
     * @param spaceQueryRequest 查询条件
     * @return 查询包装类
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param userLogin
     * @return 创建的空间id
     */
    Long addSpace(SpaceAddRequest spaceAddRequest, User userLogin);


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
     * 删除空间（同时删除空间内图片、成员关联，并清理对象存储）
     *
     * @param spaceId   空间 id
     * @param loginUser 登录用户
     * @return true 表示删除成功
     */
    boolean deleteSpace(Long spaceId, User loginUser);

}
