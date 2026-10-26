package com.normal.picturebackend.infrastucture.common;

import com.normal.picturebackend.infrastucture.exception.ErrorCode;

/**
 * 结果包装工具类
 *
 * @author normal
 * @date 2025年04月15日 上午12:09
 */
public class ResultUtils {

    /**
     * 成功封装响应
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 数据封装响应对象
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<T>(ErrorCode.SUCCESS.getCode(), data, ErrorCode.SUCCESS.getMessage());
    }

    /**
     * 失败封装响应
     *
     * @param errorCode 错误代码
     * @param <T>       数据类型
     * @return 失败响应对象
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<T>(errorCode);
    }

    /**
     * 失败封装响应
     *
     * @param code    失败代码
     * @param message 失败信息
     * @param <T>     数据类型
     * @return 失败响应对象
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<T>(code, null, message);
    }

    /**
     * 失败封装响应
     *
     * @param code    错误代码对象
     * @param message 错徐消息
     * @param <T>     数据类型
     * @return 失败响应对象
     */
    public static <T> BaseResponse<T> error(ErrorCode code, String message) {
        return new BaseResponse<T>(code.getCode(), null, message);
    }
}
