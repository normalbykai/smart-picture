package com.normal.picturebackend.shared.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.picture.service.PictureDomainService;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.entity.SpaceUser;
import com.normal.picturebackend.domain.space.service.SpaceDomainService;
import com.normal.picturebackend.domain.space.service.SpaceUserDomainService;
import com.normal.picturebackend.domain.space.valueobject.SpaceRoleEnum;
import com.normal.picturebackend.domain.space.valueobject.SpaceTypeEnum;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.user.service.UserDomainService;
import com.normal.picturebackend.domain.user.valueobject.UserConstant;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.infrastructure.exception.ThrowUtils;
import com.normal.picturebackend.infrastructure.utils.ServletUtil;
import com.normal.picturebackend.shared.auth.valueobject.SpaceUserPermissionConstant;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 自定义权限加载实现。
 *
 * <p>根据请求参数中的 {@code spaceId} / {@code pictureId} / {@code spaceUserId} 等上下文，
 * 计算出当前登录用户在该资源上拥有的权限。</p>
 *
 * <p>安全约定：</p>
 * <ul>
 *     <li>没有空间上下文时，只授予「公共图库」的最小权限（查看、上传），不再默认授予管理员权限；</li>
 *     <li>任何解析失败都按「无权限」处理（fail-closed），避免因为异常放大权限；</li>
 *     <li>系统管理员在完成登录校验后直接拥有全部空间权限。</li>
 * </ul>
 */
@Slf4j
@Component
public class StpInterfaceImpl implements StpInterface {

    /**
     * 公共图库的最小权限集合：允许浏览与上传，不允许编辑/删除他人图片
     */
    private static final List<String> PUBLIC_GALLERY_PERMISSIONS = List.of(
            SpaceUserPermissionConstant.PICTURE_VIEW,
            SpaceUserPermissionConstant.PICTURE_UPLOAD
    );

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private PictureDomainService pictureDomainService;

    @Resource
    private SpaceDomainService spaceDomainService;

    @Resource
    private SpaceUserDomainService spaceUserDomainService;

    /**
     * 服务端上下文路径（默认 /api），用于从请求 URI 中剥离前缀
     */
    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 返回指定账号拥有的权限集合
     *
     * @param loginId   当前登录的用户 id
     * @param loginType 登录类型（只有 space 类型参与空间权限计算）
     * @return 权限集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 1. 非空间类型不参与空间权限计算
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }

        // 2. 解析登录用户；未登录直接拒绝
        User loginUser = resolveLoginUser(loginId);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR, "用户未登录");

        // 3. 系统管理员拥有全部空间权限
        List<String> adminPermissions = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        if (loginUser.isAdmin()) {
            return adminPermissions;
        }

        // 4. 解析请求上下文；解析失败按空上下文处理（只给公共图库最小权限）
        SpaceUserAuthContext context = getSpaceUserAuthContext();
        if (isAllFieldsNull(context)) {
            return PUBLIC_GALLERY_PERMISSIONS;
        }

        // 5. 直接携带空间成员对象时，按成员角色返回
        SpaceUser spaceUser = context.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

        // 6. 携带空间成员 id 时，查询成员关系后返回对应角色权限
        Long spaceUserId = context.getSpaceUserId();
        if (spaceUserId != null) {
            return getPermissionsBySpaceUserId(spaceUserId, loginUser.getId());
        }

        // 7. 解析 spaceId；没有 spaceId 时尝试通过 pictureId 反查空间
        Long spaceId = context.getSpaceId();
        if (spaceId == null) {
            Long pictureId = context.getPictureId();
            if (pictureId == null) {
                return PUBLIC_GALLERY_PERMISSIONS;
            }
            Picture picture = pictureDomainService.getById(pictureId);
            ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            spaceId = picture.getSpaceId();
            if (spaceId == null) {
                // 公共图库：图片作者拥有全部权限，其他登录用户只读
                return picture.getUserId().equals(loginUser.getId())
                        ? adminPermissions
                        : Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
            }
        }

        // 8. 根据空间类型计算权限
        Space space = spaceDomainService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        Integer spaceType = space.getSpaceType();
        if (spaceType != null && SpaceTypeEnum.PRIVATE.getValue() == spaceType) {
            // 私人空间：仅空间创建者拥有全部权限（系统管理员已在第 3 步返回）
            return loginUser.getId().equals(space.getUserId())
                    ? adminPermissions
                    : Collections.emptyList();
        }

        // 团队空间：按成员角色返回权限；非成员无权限
        SpaceUser member = spaceUserDomainService.lambdaQuery()
                .eq(SpaceUser::getSpaceId, space.getId())
                .eq(SpaceUser::getUserId, loginUser.getId())
                .one();
        if (member == null) {
            return Collections.emptyList();
        }
        return spaceUserAuthManager.getPermissionsByRole(member.getSpaceRole());
    }

    /**
     * 角色列表当前未使用，保留空实现
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * 根据空间成员 id 计算权限
     *
     * @param spaceUserId 空间成员关联 id
     * @param userId      当前登录用户 id
     * @return 权限集合
     */
    private List<String> getPermissionsBySpaceUserId(Long spaceUserId, Long userId) {
        SpaceUser target = spaceUserDomainService.getById(spaceUserId);
        ThrowUtils.throwIf(target == null, ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
        SpaceUser current = spaceUserDomainService.lambdaQuery()
                .eq(SpaceUser::getSpaceId, target.getSpaceId())
                .eq(SpaceUser::getUserId, userId)
                .one();
        if (current == null) {
            return Collections.emptyList();
        }
        return spaceUserAuthManager.getPermissionsByRole(current.getSpaceRole());
    }

    /**
     * 从 Sa-Token 会话中解析登录用户
     *
     * @param loginId 登录 id
     * @return 登录用户，解析失败返回 null
     */
    private User resolveLoginUser(Object loginId) {
        try {
            Object state = StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstant.USER_LOGIN_STATE);
            if (state instanceof User user) {
                return user;
            }
            Long userId = parseUserId(state);
            return userId == null ? null : userDomainService.getById(userId);
        } catch (Exception e) {
            log.warn("解析 Sa-Token 登录用户失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 把登录态解析为用户 id，兼容 Long / Integer / String
     *
     * @param state 登录态
     * @return 用户 id，无法解析返回 null
     */
    private Long parseUserId(Object state) {
        if (state instanceof Number number) {
            return number.longValue();
        }
        if (state instanceof String text) {
            return StrUtil.isBlank(text) ? null : Long.valueOf(text);
        }
        return null;
    }

    /**
     * 从当前请求中解析空间权限上下文
     *
     * @return 上下文对象，无法解析时返回空对象
     */
    private SpaceUserAuthContext getSpaceUserAuthContext() {
        // 1. 拿到当前请求；非 Web 线程（如定时任务）直接返回空上下文
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return new SpaceUserAuthContext();
        }
        HttpServletRequest request = attributes.getRequest();
        String contentType = StrUtil.nullToEmpty(request.getHeader(Header.CONTENT_TYPE.getValue()));

        // 2. 按请求类型解析参数
        SpaceUserAuthContext context = new SpaceUserAuthContext();
        try {
            if (contentType.toLowerCase().startsWith(ContentType.JSON.getValue())) {
                String body = ServletUtil.getBody(request);
                if (StrUtil.isNotBlank(body)) {
                    context = JSONUtil.toBean(body, SpaceUserAuthContext.class);
                }
            } else {
                context = BeanUtil.toBean(ServletUtil.getParamsMap(request), SpaceUserAuthContext.class);
            }
        } catch (Exception e) {
            log.warn("解析空间权限上下文失败，按无上下文处理: {}", e.getMessage());
            return new SpaceUserAuthContext();
        }
        if (context == null) {
            return new SpaceUserAuthContext();
        }

        // 3. 根据请求路径判断 id 字段的真实含义
        Long id = context.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestUri = request.getRequestURI();
            String path = requestUri.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(path, '/', false);
            switch (moduleName) {
                case "picture" -> context.setPictureId(id);
                case "space" -> context.setSpaceId(id);
                case "user" -> context.setUserId(id);
                // 空间成员接口只传成员关联 id，需要按 spaceUserId 解析其所属空间
                case "spaceUser" -> context.setSpaceUserId(id);
                default -> {
                    // 其他模块不处理
                }
            }
        }
        return context;
    }

    /**
     * 判断上下文对象的所有字段是否都为空
     *
     * @param object 上下文对象
     * @return true 表示没有任何可用于鉴权的字段
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true;
        }
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                .map(field -> ReflectUtil.getFieldValue(object, field))
                .allMatch(ObjectUtil::isEmpty);
    }
}
