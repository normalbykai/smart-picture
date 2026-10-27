package com.normal.picturebackend.infrastructure.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.normal.picturebackend.infrastructure.common.BaseResponse;
import com.normal.picturebackend.infrastructure.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常捕获处理。
 *
 * <p>统一把业务异常、参数校验异常、上传超限、Sa-Token 鉴权异常转换为
 * {@link BaseResponse}，避免把堆栈或框架默认错误页暴露给前端。</p>
 *
 * @author normal
 * @date 2025年04月15日 上午12:16
 */
@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     *
     * @param e 业务异常
     * @return 统一响应
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.warn("BusinessException: code={}, message={}", e.getCode(), e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * @Valid 参数校验失败
     *
     * @param ex 校验异常
     * @return 统一响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .collect(Collectors.joining("；"));
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message.isBlank() ? "参数错误" : message);
    }

    /**
     * 上传文件超过 Spring 限制
     *
     * @param e 上传超限异常
     * @return 统一响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public BaseResponse<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过限制: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
    }

    /**
     * 其他运行时异常
     *
     * @param e 运行时异常
     * @return 统一响应
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        // Sa-Token 的未登录/无权限异常单独转换，其余异常只记录日志，不把细节返回给前端
        if (e instanceof NotLoginException) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (e instanceof NotPermissionException) {
            return ResultUtils.error(ErrorCode.NOT_AUTH_ERROR, "无权限");
        }
        log.error("RuntimeException:", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
