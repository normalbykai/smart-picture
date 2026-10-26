package com.normal.picturebackend.shared.auth;

import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.entity.SpaceUser;
import lombok.Data;

/**
 * 表示用户在特定空间的授权上下问， 包含关联图片，空间，用户信息
 */
@Data
public class SpaceUserAuthContext {
    /**
     * 临时参数， 不同请求ID不同
     * <p>如何获取是空间还是用户呢？</p>
     * <p>可以通过请求前缀进行判断</p>
     * <p>/picture/* 表示图片id </p>
     * <p>/space 表示空间id </p>
     * <p>以此类推</p>
     */
    private Long id;

    /**
     * 空间ID 如果有空间ID则查询该空间 -> 空间用户关联 -> 角色权限
     */
    private Long spaceId;

    /**
     * 用户ID 如果有则查询用户 -> 用户角色关联 -> 角色权限
     */
    private Long userId;

    /**
     * 空间用户id
     */
    private Long spaceUserId;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 图片信息 -> 空间id -> 空间用户关联 -> 角色权限
     */
    private Picture picture;

    /**
     * 空间信息 -> 空间用户关联 -> 角色权限
     */
    private Space space;

    /**
     * 空间用户关联信息 -> 角色权限
     */
    private SpaceUser spaceUser;
}
