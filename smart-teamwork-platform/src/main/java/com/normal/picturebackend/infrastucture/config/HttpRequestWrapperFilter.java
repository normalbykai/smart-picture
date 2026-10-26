package com.normal.picturebackend.infrastucture.config;

import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * 请求包装过滤器。
 *
 * <p>空间权限校验需要从 JSON body 中解析 spaceId / pictureId，
 * 但请求体只能读取一次，因此对 JSON 请求包一层可重复读取的 {@link RequestWrapper}。</p>
 *
 * <p>注意：Content-Type 可能带 charset（如 application/json;charset=UTF-8），
 * 这里必须用 startsWith 判断，否则 body 会被权限拦截器提前读空，导致 @RequestBody 解析失败。</p>
 *
 * @author pine
 */
@Order(1)
@Component
public class HttpRequestWrapperFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request instanceof HttpServletRequest servletRequest) {
            String contentType = servletRequest.getHeader(Header.CONTENT_TYPE.getValue());
            if (contentType != null && contentType.toLowerCase().startsWith(ContentType.JSON.getValue())) {
                chain.doFilter(new RequestWrapper(servletRequest), response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
