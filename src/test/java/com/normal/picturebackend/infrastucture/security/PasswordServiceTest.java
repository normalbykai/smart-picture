package com.normal.picturebackend.infrastucture.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 密码服务单元测试。
 *
 * <p>覆盖：BCrypt 加密/校验、旧版 MD5 兼容、错误密码拒绝、哈希升级判断。</p>
 */
class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    @DisplayName("BCrypt 加密后可以正确校验，且同一密码两次加密结果不同（随机盐）")
    void bcryptEncodeAndMatch() {
        String rawPassword = "123456";
        String first = passwordService.encode(rawPassword);
        String second = passwordService.encode(rawPassword);

        assertNotEquals(first, second, "BCrypt 应使用随机盐，两次结果不能相同");
        assertTrue(passwordService.matches(rawPassword, first));
        assertTrue(passwordService.matches(rawPassword, second));
        assertFalse(passwordService.matches("654321", first));
    }

    @Test
    @DisplayName("旧版固定盐 MD5 密码仍然可以登录，并会被标记为需要升级")
    void legacyMd5StillWorks() {
        String rawPassword = "123456";
        String legacyHash = passwordService.legacyMd5(rawPassword);

        assertTrue(passwordService.matches(rawPassword, legacyHash));
        assertFalse(passwordService.needsUpgrade(passwordService.encode(rawPassword)),
                "BCrypt 哈希不需要升级");
        assertTrue(passwordService.needsUpgrade(legacyHash), "MD5 哈希需要升级为 BCrypt");
    }

    @Test
    @DisplayName("空密码、空哈希、非法哈希都应校验失败")
    void invalidInputRejected() {
        assertFalse(passwordService.matches("", "any"));
        assertFalse(passwordService.matches("123456", ""));
        assertFalse(passwordService.matches("123456", "not-a-hash"));
    }
}
