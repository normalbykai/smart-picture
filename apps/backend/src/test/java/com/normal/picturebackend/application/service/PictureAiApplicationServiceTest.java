package com.normal.picturebackend.application.service;

import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.picture.repository.PictureRepository;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.repository.SpaceRepository;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.infrastructure.ai.*;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.shared.auth.SpaceUserAuthManager;
import com.normal.picturebackend.shared.auth.valueobject.SpaceUserPermissionConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PictureAiApplicationServiceTest {
    private final PictureRepository pictures = mock(PictureRepository.class);
    private final SpaceRepository spaces = mock(SpaceRepository.class);
    private final SpaceUserAuthManager permissions = mock(SpaceUserAuthManager.class);
    private final AiImageUrlSigner signer = mock(AiImageUrlSigner.class);
    private final AiServiceClient ai = mock(AiServiceClient.class);
    private final PictureAiApplicationService service = new PictureAiApplicationService(pictures, spaces, permissions, signer, ai);
    private User user;
    private Picture picture;

    @BeforeEach
    void setup() {
        user = new User(); user.setId(1L); user.setUserRole("user");
        picture = new Picture(); picture.setId(2L); picture.setUserId(1L);
        picture.setOriginUrl("original-url"); picture.setUrl("display-url");
        when(pictures.getById(2L)).thenReturn(picture);
    }

    @Test
    void deniesAnonymousAndNonOwnerBeforeSigning() {
        assertThrows(BusinessException.class, () -> service.analyze(2L, null, "trace"));
        user.setId(3L);
        assertEquals(40101, assertThrows(BusinessException.class,
                () -> service.analyze(2L, user, "trace")).getCode());
        verifyNoInteractions(signer, ai);
    }

    @Test
    void usesOriginalImageAndOnlyReturnsSuggestions() {
        AiImageAnalysisResult expected = new AiImageAnalysisResult("标题", List.of("风景"));
        when(signer.sign("original-url")).thenReturn("temporary-url");
        when(ai.analyzeImage("temporary-url", "trace")).thenReturn(expected);
        assertSame(expected, service.analyze(2L, user, "trace"));
        verify(pictures).getById(2L);
        verifyNoMoreInteractions(pictures);
    }

    @Test
    void resolvesSpaceFromPersistedPictureAndRequiresEditPermission() {
        picture.setSpaceId(7L);
        Space space = new Space(); space.setId(7L);
        when(spaces.getById(7L)).thenReturn(space);
        when(permissions.getPermissionsList(space, user)).thenReturn(List.of("picture:view"));
        assertThrows(BusinessException.class, () -> service.analyze(2L, user, "trace"));
        verifyNoInteractions(signer, ai);
        when(permissions.getPermissionsList(space, user)).thenReturn(List.of(SpaceUserPermissionConstant.PICTURE_EDIT));
        when(signer.sign("original-url")).thenReturn("temporary-url");
        service.analyze(2L, user, "trace");
        verify(ai).analyzeImage("temporary-url", "trace");
    }

    @Test
    void missingImageDoesNotCallAi() {
        assertEquals(40400, assertThrows(BusinessException.class,
                () -> service.analyze(99L, user, "trace")).getCode());
        verifyNoInteractions(signer, ai);
    }
}
