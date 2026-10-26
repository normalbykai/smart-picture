package com.normal.picturebackend.interfaces.assembler;

import cn.hutool.core.bean.BeanUtil;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.normal.picturebackend.interfaces.dto.user.UserAddRequest;
import com.normal.picturebackend.interfaces.dto.user.UserEditRequest;
import com.normal.picturebackend.interfaces.dto.user.UserUpdateRequest;
import com.normal.picturebackend.interfaces.vo.user.UserVO;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;

/**
 * 用户转换
 */
@Slf4j
public class UserAssembler {
    public static User toUserEntity(UserAddRequest userAddRequest) {
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        return user;
    }

    public static User toUserEntity(UserUpdateRequest userUpdateRequest) {
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        return user;
    }

    public static UserVO toUserVO(User user) {
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }


    public static <S, T> T toUserEntity(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }

        // 进行转换
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtil.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            log.error("对象转换异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "对象转换异常");
        }
    }

}
