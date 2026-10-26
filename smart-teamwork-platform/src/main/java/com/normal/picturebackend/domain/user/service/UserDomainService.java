package com.normal.picturebackend.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.normal.picturebackend.domain.user.valueobject.UserConstant;
import com.normal.picturebackend.interfaces.dto.user.UserQueryRequest;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.interfaces.vo.user.UserLoginVO;
import com.normal.picturebackend.interfaces.vo.user.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * @author normal
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-04-16 23:38:53
 */
public interface UserDomainService extends IService<User>, UserConstant {

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 重复密码
     * @return 用户注册的id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      httpRequest 请求设置cookie
     * @return 用户脱敏对象
     */
    UserLoginVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request HttpRequest
     * @return 登录的用户信息
     */
    User getUserLogin(HttpServletRequest request);

    /**
     * 用户注销登录
     *
     * @param request HttpRequest
     * @return 注销状态
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取用户脱敏后的数据
     *
     * @param user 用户信息
     * @return 用户脱敏后的信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取用户脱敏后的数据列表
     *
     * @param users 用户信息列表
     * @return 用户脱敏后的信息列表
     */
    List<UserVO> getUserListVO(List<User> users);

    /**
     * 获取加密的密码
     *
     * @param password 密码
     * @return 加密的密码
     */
    String getEncryptPassword(String password);

    /**
     * 将查询参数 转换为 Wrapper对象
     * @param queryRequest 查询参数
     * @return wrapper对象
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest queryRequest);


    /**
     * 添加用户
     *
     * @param user
     * @return
     */
    long addUser(User user);

    /**
     * 通过id 获取用户
     *
     * @param id
     * @return
     */
    User getUserById(long id);
}
