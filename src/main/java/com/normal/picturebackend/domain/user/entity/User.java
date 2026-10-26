package com.normal.picturebackend.domain.user.entity;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;
import com.normal.picturebackend.domain.user.valueobject.UserRoleEnum;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.normal.picturebackend.interfaces.vo.user.UserVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户数据库对象
 *
 * @TableName user 对应数据库的表名
 */
@TableName(value = "user")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;


    /**
     * 序列化
     */
    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 校验用户是管理员
     */
    public boolean isAdmin() {
        return UserRoleEnum.ADMIN.getValue().equals(this.getUserRole());
    }

    /**
     * 校验用户不是管理员
     */
    public boolean isNotAdmin() {
        return !isAdmin();
    }

    /**
     * 校验用户注册参数
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     */
    public static void validUserRegister(String userAccount, String userPassword, String checkPassword) {
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }

        if (userPassword.length() < 5 || checkPassword.length() < 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }

        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
    }

    /**
     * 校验用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     */
    public static void validUserLogin(String userAccount, String userPassword) {
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
    }

    /**
     * 转换为VO
     */
    public UserVO convertToVO() {
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(this, userVO);
        return userVO;
    }
}