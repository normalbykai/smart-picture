package com.normal.picturebackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.normal.picturebackend.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.normal.picturebackend.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.normal.picturebackend.domain.space.entity.SpaceUser;
import com.normal.picturebackend.interfaces.vo.spaceuser.SpaceUserVO;

import java.util.List;

/**
* @author norma
* @description 针对表【space_user(空间用户关联表)】的数据库操作Service
* @createDate 2025-06-08 00:13:19
*/
public interface SpaceUserApplicationService extends IService<SpaceUser> {

    /**
     * 转换SpaceUser对象为VO
     *
     */
    SpaceUserVO converseSpaceUserToVO(SpaceUser spaceUser);

    /**
     * 将Space集合对象 转换为VO
     * @return VO集合
     */
    List<SpaceUserVO> getSpaceVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询条件
     * @param spaceQueryRequest 查询条件
     * @return 查询包装类
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceQueryRequest);

    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest
     * @return 成员加入空间关联表的ID
     */
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员信息
     *
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);
}
