package com.normal.picturebackend.shared.ws;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.picture.service.PictureDomainService;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.service.SpaceDomainService;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.user.service.UserDomainService;
import com.normal.picturebackend.shared.auth.SpaceUserAuthManager;
import com.normal.picturebackend.shared.auth.valueobject.SpaceUserPermissionConstant;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 握手拦截器。
 *
 * <p>建立协同连接前校验：</p>
 * <ol>
 *     <li>必须携带 pictureId；</li>
 *     <li>必须是登录用户；</li>
 *     <li>必须对图片有编辑权限：公共图库看作者/管理员，私人空间看创建者，团队空间看 PICTURE_EDIT 权限。</li>
 * </ol>
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    private final UserDomainService userDomainService;
    private final PictureDomainService pictureDomainService;
    private final SpaceUserAuthManager spaceUserAuthManager;
    private final SpaceDomainService spaceDomainService;

    public WsHandshakeInterceptor(UserDomainService userDomainService,
                                  PictureDomainService pictureDomainService,
                                  SpaceUserAuthManager spaceUserAuthManager,
                                  SpaceDomainService spaceDomainService) {
        this.userDomainService = userDomainService;
        this.pictureDomainService = pictureDomainService;
        this.spaceUserAuthManager = spaceUserAuthManager;
        this.spaceDomainService = spaceDomainService;
    }

    /**
     * 握手前校验登录态与图片编辑权限
     *
     * @param request    握手请求
     * @param response   握手响应
     * @param wsHandler  WebSocket 处理器
     * @param attributes 会话属性
     * @return true 表示允许建立连接
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequestWrapper)) {
            return false;
        }
        HttpServletRequest servletRequest = servletRequestWrapper.getServletRequest();

        // 1. 校验 pictureId 参数
        String pictureIdText = servletRequest.getParameter("pictureId");
        if (StrUtil.isBlank(pictureIdText)) {
            log.warn("WebSocket 握手失败：缺少 pictureId");
            return false;
        }
        Long pictureId;
        try {
            pictureId = Long.valueOf(pictureIdText);
        } catch (NumberFormatException e) {
            log.warn("WebSocket 握手失败：pictureId 格式不正确: {}", pictureIdText);
            return false;
        }

        // 2. 校验登录态
        User loginUser;
        try {
            loginUser = userDomainService.getUserLogin(servletRequest);
        } catch (Exception e) {
            log.warn("WebSocket 握手失败：用户未登录");
            return false;
        }
        if (ObjUtil.isEmpty(loginUser)) {
            log.warn("WebSocket 握手失败：用户未登录");
            return false;
        }

        // 3. 校验图片是否存在
        Picture picture = pictureDomainService.getById(pictureId);
        if (picture == null) {
            log.warn("WebSocket 握手失败：图片不存在, pictureId={}", pictureId);
            return false;
        }

        // 4. 校验编辑权限
        if (!canEditPicture(picture, loginUser)) {
            log.warn("WebSocket 握手失败：用户 {} 没有图片 {} 的编辑权限", loginUser.getId(), pictureId);
            return false;
        }

        // 5. 附加会话属性，供处理器使用
        attributes.put("user", loginUser);
        attributes.put("userId", loginUser.getId());
        attributes.put("pictureId", pictureId);
        return true;
    }

    /**
     * 判断用户是否拥有图片编辑权限
     *
     * @param picture   图片
     * @param loginUser 登录用户
     * @return true 表示允许协同编辑
     */
    private boolean canEditPicture(Picture picture, User loginUser) {
        Long spaceId = picture.getSpaceId();
        // 公共图库：仅图片作者或系统管理员可编辑
        if (spaceId == null) {
            return loginUser.isAdmin() || picture.getUserId().equals(loginUser.getId());
        }
        // 私人/团队空间：查询空间权限
        Space space = spaceDomainService.getById(spaceId);
        if (space == null) {
            log.warn("WebSocket 握手失败：图片关联的空间不存在, spaceId={}", spaceId);
            return false;
        }
        List<String> permissions = spaceUserAuthManager.getPermissionsList(space, loginUser);
        return permissions.contains(SpaceUserPermissionConstant.PICTURE_EDIT);
    }

    /**
     * 握手完成后无需额外处理
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }
}
