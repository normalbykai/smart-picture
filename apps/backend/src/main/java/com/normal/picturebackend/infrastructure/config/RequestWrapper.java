package com.normal.picturebackend.infrastructure.config;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 包装请求，使 InputStream 可以重复读取。
 *
 * <p>空间权限拦截器需要读取 JSON body 解析 spaceId / pictureId，
 * 而 Controller 的 {@code @RequestBody} 也需要读取 body，因此这里把 body 缓存为字节数组，
 * 每次 {@link #getInputStream()} 都返回一个新的流。</p>
 *
 * @author pine
 */
@Slf4j
public class RequestWrapper extends HttpServletRequestWrapper {

    /**
     * 缓存的请求体（UTF-8 字节）
     */
    private final byte[] body;

    public RequestWrapper(HttpServletRequest request) {
        super(request);
        byte[] bytes = new byte[0];
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) > 0) {
                builder.append(buffer, 0, read);
            }
            bytes = builder.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("读取请求体失败: {}", e.getMessage());
        }
        this.body = bytes;
    }

    /**
     * 每次返回一个新的可读流，保证 body 可以被重复读取
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 同步流，无需实现
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }

            @Override
            public int read(byte[] b, int off, int len) {
                return byteArrayInputStream.read(b, off, len);
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    /**
     * 获取缓存的请求体字符串
     *
     * @return 请求体
     */
    public String getBody() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
