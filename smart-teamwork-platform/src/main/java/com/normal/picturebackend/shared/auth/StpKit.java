package com.normal.picturebackend.shared.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

@Component
public class StpKit {

    public static final String SPACE_TYPE = "space";

    /**
     * 默认原生会话对象，
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    /**
     * Space 会话对象 , 管理Space表中所有账户的登录、权限认证
     */
    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);

}
