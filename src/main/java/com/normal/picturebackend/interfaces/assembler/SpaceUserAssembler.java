package com.normal.picturebackend.interfaces.assembler;

import cn.hutool.core.bean.BeanUtil;
import com.normal.picturebackend.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.normal.picturebackend.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.normal.picturebackend.domain.space.entity.SpaceUser;

/**
 * 空间转换器
 */
public class SpaceUserAssembler {

    public static SpaceUser toSpaceEntity(SpaceUserQueryRequest spaceUserQueryRequest) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserQueryRequest, spaceUser);
        return spaceUser;
    }

    public static SpaceUser toSpaceEntity(SpaceUserAddRequest spaceUserAddRequest) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddRequest, spaceUser);
        return spaceUser;
    }
}
