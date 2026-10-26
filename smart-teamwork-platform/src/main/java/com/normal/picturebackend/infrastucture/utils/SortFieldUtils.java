package com.normal.picturebackend.infrastucture.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Set;

/**
 * 排序字段安全工具。
 *
 * <p>MyBatis-Plus 的 {@code orderBy} 会把字段名直接拼进 SQL，如果允许前端任意传值，
 * 就存在 SQL 注入风险。因此所有排序字段必须经过白名单校验，非法字段回退到默认字段。</p>
 *
 * @author norma
 */
@Slf4j
public final class SortFieldUtils {

    /**
     * 工具类不允许实例化
     */
    private SortFieldUtils() {
    }

    /**
     * 校验并返回安全的排序字段
     *
     * @param sortField     前端传入的排序字段
     * @param allowedFields 允许排序的字段白名单
     * @param defaultField  默认排序字段（白名单为空时使用）
     * @return 白名单内的字段，或默认字段
     */
    public static String resolve(String sortField, Set<String> allowedFields, String defaultField) {
        if (StrUtil.isBlank(sortField)) {
            return defaultField;
        }
        String candidate = sortField.trim();
        if (allowedFields.contains(candidate)) {
            return candidate;
        }
        // 记录非法排序字段，便于发现扫描/注入行为
        log.warn("拒绝非法排序字段: {}", candidate);
        return defaultField;
    }

    /**
     * 将前端的排序方向统一转换为是否升序
     *
     * <p>兼容 {@code asc/ascend} 与 {@code desc/descend} 两种写法，默认降序。</p>
     *
     * @param sortOrder 排序方向
     * @return true 表示升序
     */
    public static boolean isAsc(String sortOrder) {
        if (StrUtil.isBlank(sortOrder)) {
            return false;
        }
        String value = sortOrder.trim().toLowerCase(Locale.ROOT);
        return "asc".equals(value) || "ascend".equals(value);
    }
}
