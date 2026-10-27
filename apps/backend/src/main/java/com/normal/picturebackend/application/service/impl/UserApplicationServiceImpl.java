package com.normal.picturebackend.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.normal.picturebackend.domain.user.service.UserDomainService;
import com.normal.picturebackend.infrastructure.manager.file.PictureFileManager;
import com.normal.picturebackend.infrastructure.manager.file.model.UploadPictureResult;
import com.normal.picturebackend.infrastructure.mapper.UserMapper;
import com.normal.picturebackend.interfaces.dto.user.UserLoginRequest;
import com.normal.picturebackend.interfaces.dto.user.UserQueryRequest;
import com.normal.picturebackend.interfaces.vo.user.UserLoginVO;
import com.normal.picturebackend.interfaces.vo.user.UserVO;
import com.normal.picturebackend.application.service.UserApplicationService;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.infrastructure.exception.ThrowUtils;
import com.normal.picturebackend.interfaces.dto.user.UserRegisterRequest;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author norma
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-04-16 23:38:53
 */
@Service
@Slf4j
public class UserApplicationServiceImpl extends ServiceImpl<UserMapper, User> implements UserApplicationService {
    
    @Resource
    private UserDomainService userDomainService;

    @Resource
    private PictureFileManager pictureFileManager;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return 用户id
     */
    @Override
    @Transactional
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        // 1. 用户注册参数校验
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        User.validUserRegister(userAccount, userPassword, checkPassword);
        // 2. 注册数据持久化, 返回注册成功用户ID;
        return userDomainService.userRegister(userAccount, userPassword, checkPassword);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return 用户脱敏后的信息
     */
    @Override
    public UserLoginVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        // 1. 校验用户登录参数格式
        User.validUserLogin(userAccount, userPassword);
        // 2. 开始用户登录, 返回登录用户信息;
        return userDomainService.userLogin(userAccount, userPassword, request);
    }

    /**
     * 获取当前登录的用户信息
     *
     * @param request HttpRequest
     * @return 登录的用户信息
     */
    @Override
    public User getUserLogin(HttpServletRequest request) {
       return userDomainService.getUserLogin(request);
    }

    /**
     * 用户注销登录
     * @param request HttpRequest
     * @return 注销状态
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        userDomainService.userLogout(request);
        return true;
    }

    /**
     * 获取用户脱敏后的信息
     *
     * @param user 用户信息
     * @return 用户脱敏后的信息
     */
    @Override
    public UserVO getUserVO(User user) {
        return userDomainService.getUserVO(user);
    }

    /**
     * 获取用户脱敏后的信息列表
     *
     * @param users 用户信息列表
     * @return 用户脱敏后的信息列表
     */
    @Override
    public List<UserVO> getUserListVO(List<User> users) {
      return userDomainService.getUserListVO(users);
    }

    /**
     * 密码加密
     *
     * @param passwd 密码
     * @return 加密后的密码
     */
    public String getEncryptPassword(String passwd) {
        return userDomainService.getEncryptPassword(passwd);
    }

    /**
     * 获取查询包装器
     *
     * @param queryRequest 用户查询请求
     * @return 查询包装器
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest queryRequest) {
       return userDomainService.getQueryWrapper(queryRequest);
    }

    @Override
    public long addUser(User user) {
        return userDomainService.addUser(user);
    }

    @Override
    public User getUserById(long id) {
        return userDomainService.getUserById(id);
    }

    @Override
    public boolean removeById(Long id) {
        return userDomainService.removeById(id);
    }


    @Override
    @Transactional
    public boolean uploadUserAvatar(MultipartFile file, User userLogin) {
        // 1. 校验参数
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userLogin == null, ErrorCode.NOT_LOGIN_ERROR);

        // 2. 上传图片头像
        // COS上传前缀文件名 (头像图片统一放COS对象存储avatar/目录下)
        String uploadPathPrefix = "avatar";
        String oldAvatarUrl = userLogin.getUserAvatar();
        String uploadPictureUrl = pictureFileManager.uploadPictureAndGetPath(file, uploadPathPrefix);

        // 3. 更新数据库中用户的头像图片信息
        User user = new User();
        user.setId(userLogin.getId());
        user.setUserAvatar(uploadPictureUrl);
        boolean updateStatus = userDomainService.updateById(user);
        ThrowUtils.throwIf(!updateStatus, ErrorCode.OPERATION_ERROR);

        // 4. 数据库更新成功后再删除旧头像，避免更新失败导致原头像丢失
        if (StrUtil.isNotBlank(oldAvatarUrl) && !oldAvatarUrl.equals(uploadPictureUrl)) {
            try {
                pictureFileManager.deletePicture(oldAvatarUrl);
            } catch (Exception e) {
                // 旧头像清理失败不影响本次上传
                log.warn("删除旧头像失败: {}", oldAvatarUrl, e);
            }
        }
        return true;
    }


    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户详细信息
     * @return 用户脱敏后信息
     */
    public UserLoginVO getUserLoginVO(User user) {
        if (user == null) {
            return null;
        }
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtil.copyProperties(user, userLoginVO);
        return userLoginVO;
    }


}




