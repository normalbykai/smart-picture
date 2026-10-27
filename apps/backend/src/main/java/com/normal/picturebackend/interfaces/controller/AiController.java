package com.normal.picturebackend.interfaces.controller;

import com.normal.picturebackend.domain.user.valueobject.UserConstant;
import com.normal.picturebackend.infrastructure.ai.AiServiceClient;
import com.normal.picturebackend.infrastructure.ai.AiServiceInfo;
import com.normal.picturebackend.infrastructure.annotation.AuthCheck;
import com.normal.picturebackend.infrastructure.common.BaseResponse;
import com.normal.picturebackend.infrastructure.common.ResultUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiController {
    private final AiServiceClient aiServiceClient;

    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/info")
    public BaseResponse<AiServiceInfo> getInfo(
            @RequestHeader(value = "X-Request-ID", required = false) String suppliedRequestId,
            HttpServletResponse response) {
        String requestId = AiServiceClient.normalizeRequestId(suppliedRequestId);
        response.setHeader("X-Request-ID", requestId);
        return ResultUtils.success(aiServiceClient.getInfo(requestId));
    }
}
