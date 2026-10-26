package com.normal.picturebackend.infrastucture.interceptor;

import com.normal.picturebackend.infrastucture.annotation.AuthCheck;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.user.valueobject.UserRoleEnum;
import com.normal.picturebackend.domain.user.service.UserDomainService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    UserDomainService userDomainService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String role = authCheck.mustRole();

        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        User userLogin = userDomainService.getUserLogin(request);

        // 无需权限 直接放行
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(role);
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }

        // 以下为： 必须有该权限才能放行
        // 获得当前用户具有的权限
        UserRoleEnum currentRole = UserRoleEnum.getEnumByValue(userLogin.getUserRole());
        // 没有权限，拒绝
        if (currentRole == null) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }

        // 要求必须有管理员权限，但用户没有管理员权限，拒绝
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(currentRole)) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }

        // 通过权限校验，放行
        return joinPoint.proceed();



    }
}
