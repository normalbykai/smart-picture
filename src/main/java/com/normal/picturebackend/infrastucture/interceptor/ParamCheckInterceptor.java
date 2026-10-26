package com.normal.picturebackend.infrastucture.interceptor;

import com.normal.picturebackend.infrastucture.annotation.ParamCheck;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;

/**
 * 参数校验拦截器
 */
@Aspect
@Component
public class ParamCheckInterceptor {

    // TODO 还未实现
    @Around("@annotation(paramCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, ParamCheck paramCheck) throws Throwable {
        Object[] args = joinPoint.getArgs();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = methodSignature.getMethod().getParameters();

        for (int i = 0 ; i < args.length ; i++) {
            Parameter param = parameters[i];

        }
        return joinPoint.proceed();
    }
}
