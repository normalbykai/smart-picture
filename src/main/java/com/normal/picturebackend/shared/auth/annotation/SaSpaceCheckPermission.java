package com.normal.picturebackend.shared.auth.annotation;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.normal.picturebackend.shared.auth.StpKit;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


// 合并satoken的鉴权注解

/**
 * 空间权限认证: 必须有指定权限才能进入
 * <p>可标注在函数, 类上</p>
 */
@SaCheckPermission(type = StpKit.SPACE_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SaSpaceCheckPermission {

    /**
     * 权限值(picture:view....)
     *
     * @return 需要校验的权限吗
     */
    @AliasFor(annotation = SaCheckPermission.class)
    String[] value() default {};

    /**
     * 校验模式 AND| OR, 默认 AND
     *
     * @return
     */
    @AliasFor(annotation = SaCheckPermission.class)
    SaMode mode() default SaMode.AND;

    /**
     * 在权限校验不通过的次要选择， 两者只要其一校验成功可通过校验
     * <p>
     * 使用栗子
     * @SaSpaceCheckPermisstion(value = "picture:add", orRole = "admin")
     * 理解: 满足picture:add权限或者admin角色 只要满足一条则可通过权限
     * </p>
     *
     *
     * <p>
     * 栗子1： orRole = {"admin", "manage", "staff"}, 三个角色中其一即可通过权限
     * 栗子2： orRole = {"admin,manage,staff"} 必须满足这三个角色才能通过权限
     * </p>
     *
     * @return
     */
    @AliasFor(annotation = SaCheckPermission.class)
    String[] orRole() default {};
}
