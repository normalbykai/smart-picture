package com.normal.picturebackend.interfaces.controller;

import com.normal.picturebackend.application.service.PictureAiApplicationService;
import com.normal.picturebackend.application.service.UserApplicationService;
import com.normal.picturebackend.infrastructure.ai.AiImageAnalysisResult;
import com.normal.picturebackend.infrastructure.ai.AiServiceClient;
import com.normal.picturebackend.infrastructure.common.BaseResponse;
import com.normal.picturebackend.infrastructure.common.ResultUtils;
import com.normal.picturebackend.interfaces.dto.picture.AnalyzePictureRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/picture")
public class PictureAiController {
    private final PictureAiApplicationService analysis;
    private final UserApplicationService users;

    @PostMapping("/analyze")
    public BaseResponse<AiImageAnalysisResult> analyze(@Valid @RequestBody AnalyzePictureRequest body,
            @RequestHeader(value = "X-Request-ID", required = false) String suppliedRequestId,
            HttpServletRequest request, HttpServletResponse response) {
        String requestId = AiServiceClient.normalizeRequestId(suppliedRequestId);
        response.setHeader("X-Request-ID", requestId);
        return ResultUtils.success(analysis.analyze(body.pictureId(), users.getUserLogin(request), requestId));
    }
}
