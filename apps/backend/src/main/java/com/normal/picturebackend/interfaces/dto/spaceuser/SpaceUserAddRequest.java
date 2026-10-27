package com.normal.picturebackend.interfaces.dto.spaceuser;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 空间用户 添加空间成员请求
 */
@Data
public class SpaceUserAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -1L;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 空间角色
     */
    private String spaceRole;
}
