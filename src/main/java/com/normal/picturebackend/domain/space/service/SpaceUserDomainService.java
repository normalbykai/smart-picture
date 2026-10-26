package com.normal.picturebackend.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.normal.picturebackend.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.normal.picturebackend.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.normal.picturebackend.interfaces.vo.spaceuser.SpaceUserVO;
import com.normal.picturebackend.domain.space.entity.SpaceUser;
import jakarta.annotation.Resource;

import java.util.List;

/**
* @author norma
* @description 针对表【space_user(空间用户关联表)】的数据库操作Service
* @createDate 2025-06-08 00:13:19
*/
public interface SpaceUserDomainService extends IService<SpaceUser> {

    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest 查询条件
     * @return 查询包装类
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceQueryRequest);
}

