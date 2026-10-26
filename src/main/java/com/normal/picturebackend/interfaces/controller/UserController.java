package com.normal.picturebackend.interfaces.controller;

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
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.interfaces.vo.user.UserLoginVO;
import com.normal.picturebackend.interfaces.vo.user.UserVO;
import com.normal.picturebackend.interfaces.assembler.UserAssembler;
import com.normal.picturebackend.interfaces.dto.user.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 用户账号注册
     *
     * @param userRegisterRequest 用户账号注册请求参数
     * @return 注册用户id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userApplicationService.userRegister(userRegisterRequest));
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求对象
     * @param request          httpRequest
     * @return 用户脱敏信息
     */
    @PostMapping("/login")
    public BaseResponse<UserLoginVO> userLoginUsingPost(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        // 校验账号|密码 格式
        User.validUserLogin(userAccount, userPassword);
        return ResultUtils.success(userApplicationService.userLogin(userLoginRequest, request));
    }

    /**
     * 获取当前登录的用户
     *
     * @param request HttpRequest
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<UserVO> getUserLogin(HttpServletRequest request) {
        User userLogin = userApplicationService.getUserLogin(request);
        return ResultUtils.success(userApplicationService.getUserVO(userLogin));
    }

    /**
     * 用户注销登录
     *
     * @param request HttpRequest
     * @return 是否注销成功
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userApplicationService.userLogout(request));
    }

    /**
     * 添加用户 （仅管理员）
     *
     * @param userAddRequest 添加用户的基本信息
     * @return 添加成功的用户id
     */
    @PostMapping("/addUser")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        User user = UserAssembler.toUserEntity(userAddRequest);
        return ResultUtils.success(userApplicationService.addUser(user));
    }

    /**
     * 根据 id 获取用户 （仅管理员）
     *
     * <p>安全说明：该接口只允许管理员调用，且返回前会清空密码哈希，
     * 避免任何情况下把密码字段暴露给客户端。</p>
     *
     * @param id 用户id
     * @return 用户信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userApplicationService.getUserById(id);
        // 密码哈希永不出库返回
        user.setUserPassword(null);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取用户包装类
     *
     * @param id 用户id
     * @return 用户包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 1. 查找数据
        User user = getUserById(id).getData();
        // 2. 进行脱敏返回
        return ResultUtils.success(userApplicationService.getUserVO(user));
    }

    /**
     * 删除用户 （仅管理员）
     *
     * @param deleteRequest 要删除用户的id
     * @return 删除状态
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userApplicationService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户信息 (仅管理员)
     *
     * @param userUpdateRequest 更新用户的参数
     * @return 更新状态
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 1. 校验参数
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        // 2. 转换对象
        User user = UserAssembler.toUserEntity(userUpdateRequest);
        // 3. 更新用户信息
        boolean updateState = userApplicationService.updateById(user);
        // 4. 更新失败, 抛出异常
        ThrowUtils.throwIf(!updateState, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页查询用户信息 根据用户字段值 (仅管理员)
     *
     * @param queryRequest 用户字段
     * @return 分页用户信息集合
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> pageList(@RequestBody UserQueryRequest queryRequest) {
        // 1. 参数校验
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        // 2. 当前页码 和 每页大小
        long currentNum = queryRequest.getCurrentNum();
        long pageSize = queryRequest.getPageSize();
        Page<User> page = userApplicationService.page(new Page<>(currentNum, pageSize),
                userApplicationService.getQueryWrapper(queryRequest));
        // 脱敏Uer 数据转换为 UserVO
        Page<UserVO> userVOPage = new Page<>(currentNum, pageSize, page.getTotal());
        List<UserVO> userListVO = userApplicationService.getUserListVO(page.getRecords());
        userVOPage.setRecords(userListVO);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 上传用户头像
     */
    @PostMapping(value = "/upload/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuthCheck
    public BaseResponse<Boolean> uploadUserAvatar(@RequestPart(value = "file") MultipartFile file,
                                               HttpServletRequest request) {
        // 获取用户信息
        User userLogin = userApplicationService.getUserLogin(request);

        // 上传用户头像并更新为最新头像
        userApplicationService.uploadUserAvatar(file, userLogin);
        return ResultUtils.success(true);
    }

    /**
     * 更新个人用户信息
     */
    @PostMapping(value = "/update/vo")
    @AuthCheck
    @Operation(summary = "更新个人用户信息", description = "更新当前登录用户的昵称与简介")
    public BaseResponse<Boolean> updateVO(@RequestBody @Valid UserEditRequest userEditRequest,
                                                  HttpServletRequest request) {
        // 1. 获取用户信息
        User userLogin = userApplicationService.getUserLogin(request);

        // 2. 将编辑请求转换为用户实体
        User editUser = UserAssembler.toUserEntity(userEditRequest, User.class);
        editUser.setId(userLogin.getId()); // 设置用户本人ID

        // 3. 更新用户信息
        userApplicationService.updateById(editUser);
        return ResultUtils.success(true);
    }


}


