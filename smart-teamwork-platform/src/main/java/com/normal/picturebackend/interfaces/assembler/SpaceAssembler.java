package com.normal.picturebackend.interfaces.assembler;

import cn.hutool.core.bean.BeanUtil;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.interfaces.dto.space.SpaceAddRequest;
import com.normal.picturebackend.interfaces.dto.user.UserAddRequest;
import com.normal.picturebackend.interfaces.vo.space.SpaceVO;

/**
 * 空间转换器
 */
public class SpaceAssembler {

    public static SpaceVO toSpaceVO(Space space) {
        return SpaceVO.objToVo(space);
    }


    public static Space toSpaceEntity(SpaceAddRequest spaceAddRequest) {
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest, space);
        return space;
    }
}
