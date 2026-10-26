package com.normal.picturebackend.infrastucture.utils;

import cn.hutool.core.util.ArrayUtil;
import jakarta.servlet.ServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

@Slf4j
public class ServletUtil {

    /**
     * 通过ServletRequest 获取到 body数据
     */
    public static String getBody(ServletRequest request) {
        try {
            return request.getReader().lines().reduce("", (acc, cur) -> acc + cur);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 通过ServletRequest 获取到 params 数据 转换为一个Map对象
     */
    public static Map<String, String> getParamsMap(ServletRequest request) {
        Map<String, String> params = new HashMap<>();
        getParams(request).entrySet().forEach(entry -> {
            params.put(entry.getKey(), ArrayUtil.join(entry.getValue(), ","));
        });

        return params;
    }

    /**
     * 获取params参数
     * @param request
     * @return
     */
    public static Map<String, String[]> getParams(ServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        // 设置为不能修改参数
        return Collections.unmodifiableMap(params);
    }


}
