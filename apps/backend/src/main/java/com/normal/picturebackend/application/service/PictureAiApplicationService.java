package com.normal.picturebackend.application.service;

import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.picture.repository.PictureRepository;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.repository.SpaceRepository;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.infrastructure.ai.AiImageAnalysisResult;
import com.normal.picturebackend.infrastructure.ai.AiImageUrlSigner;
import com.normal.picturebackend.infrastructure.ai.AiServiceClient;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.shared.auth.SpaceUserAuthManager;
import com.normal.picturebackend.shared.auth.valueobject.SpaceUserPermissionConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PictureAiApplicationService {
    private final PictureRepository pictures;
    private final SpaceRepository spaces;
    private final SpaceUserAuthManager permissions;
    private final AiImageUrlSigner signer;
    private final AiServiceClient ai;

    public AiImageAnalysisResult analyze(Long pictureId, User user, String requestId) {
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (pictureId == null || pictureId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture picture = pictures.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 从数据库图片反查所属空间，不信任请求体中的 spaceId 或角色信息。
        boolean allowed;
        if (picture.getSpaceId() == null) {
            allowed = user.isAdmin() || Objects.equals(user.getId(), picture.getUserId());
        } else {
            Space space = spaces.getById(picture.getSpaceId());
            if (space == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            }
            allowed = permissions.getPermissionsList(space, user)
                    .contains(SpaceUserPermissionConstant.PICTURE_EDIT);
        }
        if (!allowed) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "需要图片编辑权限才能执行智能分析");
        }
        String url = StringUtils.hasText(picture.getOriginUrl()) ? picture.getOriginUrl() : picture.getUrl();
        return ai.analyzeImage(signer.sign(url), requestId);
    }
}
