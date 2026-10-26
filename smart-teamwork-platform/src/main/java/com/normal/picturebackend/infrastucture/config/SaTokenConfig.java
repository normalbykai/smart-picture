package com.normal.picturebackend.infrastucture.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * satoken 配置类
 */
@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    // 注册satoken注解是拦截器
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册注解satoken拦截器， 打开注解鉴权功能
        registry.addInterceptor(new SaInterceptor( )).addPathPatterns("/**");
        log.info("SaToken注解拦截器注册成功，开启注解鉴权功能。");
    }

    @PostConstruct
    public void rewriteStrategy() {
        // 重写Sa-token的注解处理器， 增加注解合并功能
        SaAnnotationStrategy.instance.getAnnotation = AnnotatedElementUtils::getMergedAnnotation;
    }

}
