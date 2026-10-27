package com.normal.picturebackend.infrastructure.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author normal
 * @date 2025年04月14日 下午11:54
 */

@Getter
@AllArgsConstructor
public enum ErrorCode {
    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NOT_AUTH_ERROR(40101, "未授权"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败");


    /**
     * 状态码
     */
    private final int code;
    /**
     * 描述信息
     */
    private final String message;




}
