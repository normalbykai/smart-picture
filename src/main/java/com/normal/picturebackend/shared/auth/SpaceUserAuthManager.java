package com.normal.picturebackend.shared.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.normal.picturebackend.shared.auth.model.SpaceUserAuthConfig;
import com.normal.picturebackend.shared.auth.model.SpaceUserRole;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.entity.SpaceUser;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.space.valueobject.SpaceRoleEnum;
import com.normal.picturebackend.domain.space.valueobject.SpaceTypeEnum;
import com.normal.picturebackend.domain.space.service.SpaceUserDomainService;
import com.normal.picturebackend.domain.user.service.UserDomainService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 空间用户权限管理
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private SpaceUserDomainService spaceUserDomainService;

    @Resource
    private UserDomainService userDomainService;


    /**
     * 空间用户角色/权限管理配置
     */
    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }


    /**
     * 根据角色获取权限列表
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return new ArrayList<>();
        }
        // 找到匹配的角色
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(item -> spaceUserRole.equals(item.getKey()))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return new ArrayList<>();
        }
        return role.getPermissions();
    }

    /**
     * 获取权限列表
     */
    public List<String> getPermissionsList(Space space, User loginUser) {
        // 校验登录
        if (loginUser == null) {
            return Collections.emptyList();
        }

        // 空间管理员权限 （所有权限）
        List<String> SPACE_ADMIN_PERMISSION = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());

        // 公共图库
        if (space == null) {
            boolean SystemAdmin = loginUser.isAdmin();
            // 为图片创建者或系统管理员给予全部权限
            if (SystemAdmin) {
                return SPACE_ADMIN_PERMISSION;
            }
            // 无权限
            return Collections.emptyList();
        }

        // 校验空间类型 团队/私人
        SpaceTypeEnum spaceType = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceType == null) {
            // 未找到空间类型 无权限
            return Collections.emptyList();
        }
        // 根据空间类型获取对应的权限
        switch (spaceType) {
            case PRIVATE -> {
                // 私人空间仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || loginUser.isAdmin()) {
                    return SPACE_ADMIN_PERMISSION;
                } else {
                    return Collections.emptyList();
                }
            }
            case TEEM -> {
                if (loginUser.isAdmin()) {
                    return SPACE_ADMIN_PERMISSION;
                }
                // 团队空间 查询SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserDomainService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return Collections.emptyList();
                }
                return getPermissionsByRole(spaceUser.getSpaceRole());
            }
        }

        // 都没拿到权限则无权限
        return Collections.emptyList();



    }
}
