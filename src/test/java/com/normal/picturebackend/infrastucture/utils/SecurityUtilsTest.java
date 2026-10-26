package com.normal.picturebackend.infrastucture.utils;

import com.normal.picturebackend.infrastucture.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安全工具单元测试。
 *
 * <p>覆盖：排序字段白名单、SSRF 内网地址拦截、非法协议拦截。</p>
 */
class SecurityUtilsTest {

    @Test
    @DisplayName("排序字段只允许白名单内的值，非法值回退到默认字段")
    void sortFieldWhitelist() {
        Set<String> allowed = Set.of("createTime", "name");

        assertEquals("createTime", SortFieldUtils.resolve("createTime", allowed, "createTime"));
        assertEquals("createTime", SortFieldUtils.resolve("id; drop table picture", allowed, "createTime"));
        assertEquals("createTime", SortFieldUtils.resolve(null, allowed, "createTime"));
    }

    @Test
    @DisplayName("排序方向兼容 asc/ascend，其余按降序处理")
    void sortOrderParsing() {
        assertTrue(SortFieldUtils.isAsc("asc"));
        assertTrue(SortFieldUtils.isAsc("ASCEND"));
        assertFalse(SortFieldUtils.isAsc("desc"));
        assertFalse(SortFieldUtils.isAsc(null));
    }

    @Test
    @DisplayName("内网/回环/云元数据地址应被判定为禁止访问")
    void blockedAddresses() throws Exception {
        assertTrue(UrlSecurityValidator.isBlockedAddress(InetAddress.getByName("127.0.0.1")));
        assertTrue(UrlSecurityValidator.isBlockedAddress(InetAddress.getByName("10.0.0.1")));
        assertTrue(UrlSecurityValidator.isBlockedAddress(InetAddress.getByName("172.16.0.1")));
        assertTrue(UrlSecurityValidator.isBlockedAddress(InetAddress.getByName("192.168.1.1")));
        assertTrue(UrlSecurityValidator.isBlockedAddress(InetAddress.getByName("169.254.169.254")));
        assertTrue(UrlSecurityValidator.isBlockedAddress(InetAddress.getByName("100.64.0.1")));
    }

    @Test
    @DisplayName("公网地址不应被误杀")
    void publicAddressAllowed() throws Exception {
        assertFalse(UrlSecurityValidator.isBlockedAddress(InetAddress.getByName("8.8.8.8")));
        assertFalse(UrlSecurityValidator.isBlockedAddress(InetAddress.getByName("172.32.0.1")));
    }

    @Test
    @DisplayName("非 http/https 协议与内网 URL 必须被拒绝")
    void invalidUrlRejected() {
        assertThrows(BusinessException.class,
                () -> UrlSecurityValidator.validateAndParse("file:///etc/passwd"));
        assertThrows(BusinessException.class,
                () -> UrlSecurityValidator.validateAndParse("http://127.0.0.1/avatar.jpg"));
        assertThrows(BusinessException.class,
                () -> UrlSecurityValidator.validateAndParse("http://localhost/avatar.jpg"));
    }
}
