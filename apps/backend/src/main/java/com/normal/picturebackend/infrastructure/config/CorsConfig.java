package com.normal.picturebackend.infrastructure.config;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * 跨域配置。
 *
 * <p>出于安全考虑，不再允许任意来源携带 Cookie 访问接口。
 * 允许的来源通过 {@code app.cors.allowed-origin-patterns} 配置，
 * 默认只放行本地开发常用的 localhost / 127.0.0.1。</p>
 *
 * <p>生产环境请通过环境变量 {@code CORS_ALLOWED_ORIGINS} 配置真实域名，
 * 不要使用 {@code *}。</p>
 *
 * @author normal
 */
@Slf4j
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 允许跨域的来源模式，多个用英文逗号分隔
     */
    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOriginPatterns;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] patterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toArray(String[]::new);
        if (patterns.length == 0) {
            log.warn("未配置任何跨域来源，浏览器跨域请求将被拒绝");
        }
        registry.addMapping("/**")
                // 允许发送 Cookie
                .allowCredentials(true)
                // 只放行配置中声明的来源（使用 patterns 以兼容端口通配）
                .allowedOriginPatterns(patterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                // 预检结果缓存 1 小时，减少 OPTIONS 请求
                .maxAge(3600);
        log.info("跨域允许来源: {}", String.join(",", patterns));
    }
}
