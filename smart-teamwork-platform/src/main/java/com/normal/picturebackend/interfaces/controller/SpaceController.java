package com.normal.picturebackend.interfaces.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.normal.picturebackend.application.service.UserApplicationService;
import com.normal.picturebackend.infrastucture.annotation.AuthCheck;
import com.normal.picturebackend.infrastucture.common.BaseResponse;
import com.normal.picturebackend.infrastucture.common.DeleteRequest;
import com.normal.picturebackend.infrastucture.common.ResultUtils;
import com.normal.picturebackend.domain.user.valueobject.UserConstant;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.normal.picturebackend.infrastucture.exception.ThrowUtils;
import com.normal.picturebackend.shared.auth.SpaceUserAuthManager;
import com.normal.picturebackend.interfaces.dto.space.SpaceAddRequest;
import com.normal.picturebackend.interfaces.dto.space.SpaceLevel;
import com.normal.picturebackend.interfaces.dto.space.SpaceQueryRequest;
import com.normal.picturebackend.interfaces.dto.space.SpaceUpdateRequest;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.space.valueobject.SpaceLevelEnum;
import com.normal.picturebackend.interfaces.vo.space.SpaceVO;
import com.normal.picturebackend.application.service.SpaceApplicationService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 空间控制层
 * 
 */
@RestController
@Slf4j
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    @Autowired
    private SpaceUserAuthManager spaceUserAuthManager;


    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 登录用户信息
        User userLogin = userApplicationService.getUserLogin(request);

        // 开始创建空间逻辑
        Long spaceId = spaceApplicationService.addSpace(spaceAddRequest, userLogin);
        return ResultUtils.success(spaceId);
    }

    /**
     * 删除空间。
     *
     * <p>空间内图片、成员关联的级联删除与对象存储清理统一收敛到应用服务，
     * 控制器只负责参数校验与登录用户获取。</p>
     *
     * @param deleteRequest 要删除空间的id
     * @param request       登录用户请求
     * @return true 删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 1. 参数校验
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 获取登录用户并执行删除
        User userLogin = userApplicationService.getUserLogin(request);
        boolean deleted = spaceApplicationService.deleteSpace(deleteRequest.getId(), userLogin);
        return ResultUtils.success(deleted);
    }

    /**
     * 更新空间信息 (仅管理员)
     *
     * @param spaceUpdateRequest 空间更新请求
     * @return true 为更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        // 1. 校验请求参数
        ThrowUtils.throwIf(spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 校验空间参数
        space.validate(false);
        // 填充空间级别的 最大数量和最大容量
        space.fillSpaceBySpaceLevel();

        // 2. 判断空间是否存在
        boolean exists = spaceApplicationService.lambdaQuery()
                .eq(Space::getId, spaceUpdateRequest.getId())
                .exists();
        ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "存储空间不存在");

        // 3. 更新空间 操作数据库
        boolean save = spaceApplicationService.updateById(space);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "空间更新失败，数据库错误");
        return ResultUtils.success(true);
    }

    /**
     * 获取空间信息
     *
     * @param id      空间id
     * @param request 登录用户的信息请求
     * @return 空间封装类VO
     */
    @PostMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 2. 查询该空间
        Space space = spaceApplicationService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        // 3. 封装拥有权限列表
        User userLogin = userApplicationService.getUserLogin(request);
        List<String> permissionsList = spaceUserAuthManager.getPermissionsList(space, userLogin);
        SpaceVO spaceVO = spaceApplicationService.getSpaceVO(space, request);
        spaceVO.setPermissions(permissionsList);
        // 返回封装类
        return ResultUtils.success(spaceVO);
    }




    /**
     * 分页查询空间列表 （仅管理员）
     *
     * @param spaceQueryRequest 查询参数请求
     * @return 空间分页列表
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page")
    public BaseResponse<Page<Space>> getSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取查询包装器
        QueryWrapper<Space> queryWrapper = spaceApplicationService.getQueryWrapper(spaceQueryRequest);
        // 分页查询
        Page<Space> spacePage = spaceApplicationService.page(new Page<>(spaceQueryRequest.getCurrentNum(), spaceQueryRequest.getPageSize()), queryWrapper);
        return ResultUtils.success(spacePage);
    }


    /**
     * 分页查询空间视图列表数据
     *
     */

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> getSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取查询包装器
        QueryWrapper<Space> queryWrapper = spaceApplicationService.getQueryWrapper(spaceQueryRequest);
        // 分页查询
        Page<Space> spacePage = spaceApplicationService.page(new Page<>(spaceQueryRequest.getCurrentNum(), spaceQueryRequest.getPageSize()), queryWrapper);
        Page<SpaceVO> spaceVOPage = spaceApplicationService.getSpaceVOPage(spacePage, request);
        return ResultUtils.success(spaceVOPage);
    }

    /**
     * 查询空间级别列表信息
     *
     * @return 空间级别的相关信息集合
     */
    @PostMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> list = Arrays.stream(SpaceLevelEnum.values())
                .map(el -> new SpaceLevel(el.getValue(), el.getText(), el.getMaxCount(), el.getMaxSize()))
                .toList();
        return ResultUtils.success(list);
    }




}
