package com.normal.picturebackend.interfaces.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.normal.picturebackend.application.service.UserApplicationService;
import com.normal.picturebackend.infrastructure.annotation.AuthCheck;
import com.normal.picturebackend.shared.auth.annotation.SaSpaceCheckPermission;
import com.normal.picturebackend.infrastructure.common.BaseResponse;
import com.normal.picturebackend.infrastructure.common.ResultUtils;
import com.normal.picturebackend.domain.user.valueobject.UserConstant;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.infrastructure.exception.ThrowUtils;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.application.service.SpaceAnalyzeApplicationService;
import com.normal.picturebackend.interfaces.dto.space.*;
import com.normal.picturebackend.interfaces.vo.space.*;
import com.normal.picturebackend.shared.auth.valueobject.SpaceUserPermissionConstant;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 空间分析控制层
 */
@RestController
@Slf4j
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource( name = "spaceAnalyzeApplicationServiceImpl")
    private SpaceAnalyzeApplicationService spaceAnalyzeApplicationService;

    /**
     * 获取空间的使用分析结果
     */
    @PostMapping("/usage")
    @SaSpaceCheckPermission(SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyzeResult(@RequestBody SpaceAnalyzeRequest spaceAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 拿到登录信息
        User userLogin = userApplicationService.getUserLogin(request);

        // 获取空间使用信息
        SpaceUsageAnalyzeResponse spaceUsageAnalyzeResult = spaceAnalyzeApplicationService.getSpaceUsageAnalyzeResult(spaceAnalyzeRequest, userLogin);
        return ResultUtils.success( spaceUsageAnalyzeResult);
    }

    /**
     * 获取空间的分类分析结果
     */
    @PostMapping("/category")
    @SaSpaceCheckPermission(SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyzeResult(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 拿到登录信息
        User userLogin = userApplicationService.getUserLogin(request);
        // 获取空间使用信息
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyzeResult = spaceAnalyzeApplicationService.getSpaceCategoryAnalyzeResult(spaceCategoryAnalyzeRequest, userLogin);
        return ResultUtils.success( spaceCategoryAnalyzeResult);
    }

    /**
     * 获取空间的标签分析结果
     */
    @PostMapping("/tag")
    @SaSpaceCheckPermission(SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyzeResult(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 拿到登录信息
        User userLogin = userApplicationService.getUserLogin(request);
        // 获取空间使用信息
        List<SpaceTagAnalyzeResponse> spaceTagAnalyzeResult = spaceAnalyzeApplicationService.getSpaceTagAnalyzeResult(spaceTagAnalyzeRequest, userLogin);
        return ResultUtils.success( spaceTagAnalyzeResult);
    }

    /**
     * 获取空间大小分析
     */
    @PostMapping("/size")
    @SaSpaceCheckPermission(SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyzeResult(@RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 拿到登录信息
        User userLogin = userApplicationService.getUserLogin(request);
        // 获取空间使用信息
        List<SpaceSizeAnalyzeResponse> spaceSizeAnalyzeResult = spaceAnalyzeApplicationService.getSpaceSizeAnalyzeResult(spaceSizeAnalyzeRequest, userLogin);
        return ResultUtils.success( spaceSizeAnalyzeResult);
    }

    /**
     * 获取用户上传行为分析
     */
    @PostMapping("/user")
    @SaSpaceCheckPermission(SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyzeResult(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 拿到登录信息
        User userLogin = userApplicationService.getUserLogin(request);
        // 获取空间使用信息
        List<SpaceUserAnalyzeResponse> spaceUserAnalyzeResult = spaceAnalyzeApplicationService.getSpaceUserAnalyzeResult(spaceUserAnalyzeRequest, userLogin);
        return ResultUtils.success( spaceUserAnalyzeResult);
    }

    /**
     * 获取空间使用排行榜
     * 仅管理员可查询
     */
    @PostMapping("/rank")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<Space>> getSpaceRankAnalyzeResult(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 拿到登录信息
        User userLogin = userApplicationService.getUserLogin(request);
        // 获取空间使用信息
        List<Space> spaceUserAnalyzeResult = spaceAnalyzeApplicationService.getSpaceRankAnalyzeResult(spaceRankAnalyzeRequest, userLogin);
        return ResultUtils.success( spaceUserAnalyzeResult);
    }


    
}
