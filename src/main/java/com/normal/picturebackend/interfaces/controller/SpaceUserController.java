package com.normal.picturebackend.interfaces.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ObjUtil;
import com.normal.picturebackend.application.service.UserApplicationService;
import com.normal.picturebackend.infrastucture.common.BaseResponse;
import com.normal.picturebackend.infrastucture.common.DeleteRequest;
import com.normal.picturebackend.infrastucture.common.ResultUtils;
import com.normal.picturebackend.shared.auth.valueobject.SpaceUserPermissionConstant;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.normal.picturebackend.infrastucture.exception.ThrowUtils;
import com.normal.picturebackend.shared.auth.StpKit;
import com.normal.picturebackend.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.normal.picturebackend.interfaces.dto.spaceuser.SpaceUserEditRequest;
import com.normal.picturebackend.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.normal.picturebackend.domain.space.entity.SpaceUser;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.interfaces.vo.spaceuser.SpaceUserVO;
import com.normal.picturebackend.application.service.SpaceUserApplicationService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 空间用户关联控制层
 * 
 */
@RestController
@Slf4j
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 添加成员到空间
     * @param spaceUserAddRequest
     * @param request
     * @return 空间关联的ID
     */
    @PostMapping("/add")
    @SaCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE, type = StpKit.SPACE_TYPE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceUserId = spaceUserApplicationService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(spaceUserId);
    }

    /**
     * 从空间移除成员
     *
     * @param deleteRequest 要删除空间用户关联的id
     * @param request       登录用户请求 会读取本地session获取登录信息
     * @return true 删除成功
     */
    @PostMapping("/delete")
    @SaCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE, type = StpKit.SPACE_TYPE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 参数校验：id 非法时才抛错（注意不要写成 valid()，否则合法请求会被拒绝）
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断该空间关联是否存在
        Long deleteSpaceUserId = deleteRequest.getId();
        SpaceUser oldSpaceUser = spaceUserApplicationService.getById(deleteSpaceUserId);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR, "存储空间用户关联不存在");

        // 操作数据库
        boolean result = spaceUserApplicationService.removeById(deleteSpaceUserId);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "空间用户关联删除失败，数据库错误");
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @PostMapping("/get")
    @SaCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE, type = StpKit.SPACE_TYPE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
        // 1. 校验请求参数
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = spaceUserQueryRequest.getUserId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(userId, spaceId), ErrorCode.PARAMS_ERROR);

        // 构建查询条件
        SpaceUser spaceUser = spaceUserApplicationService.getOne(spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR, "空间用户关联不存在");
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     */
    @PostMapping("/list")
    @SaCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE, type = StpKit.SPACE_TYPE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);

        List<SpaceUser> list = spaceUserApplicationService.list(
                spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest)
        );
        List<SpaceUserVO> spaceVOList = spaceUserApplicationService.getSpaceVOList(list);
        return ResultUtils.success(spaceVOList);
    }


    /**
     * 编辑成员信息 （设置权限）
     */
    @PostMapping("/edit")
    @SaCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE, type = StpKit.SPACE_TYPE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequeste) {
        ThrowUtils.throwIf(spaceUserEditRequeste == null, ErrorCode.PARAMS_ERROR);
        // 转换对象
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequeste, spaceUser);

        // 校验参数
        spaceUserApplicationService.validSpaceUser(spaceUser, false);
        // 判断是否存在
        Long id = spaceUserEditRequeste.getId();
        SpaceUser oldSpaceUser = spaceUserApplicationService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR, "空间用户关联不存在");

        // 操作数据库 修改角色权限
        boolean result = spaceUserApplicationService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "空间用户关联修改失败，数据库错误");
        return ResultUtils.success(true);
    }


    /**
     * 查询我加入的团队空间
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User userLogin = userApplicationService.getUserLogin(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(userLogin.getId());
        List<SpaceUser> list = spaceUserApplicationService.list(
                spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest)
        );
        List<SpaceUserVO> spaceVOList = spaceUserApplicationService.getSpaceVOList(list);
        return ResultUtils.success(spaceVOList);
    }

}
