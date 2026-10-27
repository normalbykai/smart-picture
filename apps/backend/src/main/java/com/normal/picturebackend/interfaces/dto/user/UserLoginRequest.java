package com.normal.picturebackend.interfaces.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录请求 数据传输对象
 */
@Data
public class UserLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -3343115749578889521L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */

    private String userPassword;
}
