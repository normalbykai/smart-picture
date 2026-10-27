package com.normal.picturebackend.infrastructure.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 密码加密与校验服务。
 *
 * <p>设计说明：</p>
 * <ul>
 *     <li>新密码统一使用 BCrypt 存储，自带随机盐，避免 MD5 被彩虹表破解；</li>
 *     <li>为了兼容历史数据，登录时仍能校验旧版「固定盐 + MD5」的密码；</li>
 *     <li>旧密码校验成功后由调用方触发一次升级，把数据库中的哈希替换为 BCrypt。</li>
 * </ul>
 *
 * @author norma
 */
@Slf4j
@Component
public class PasswordService {

    /**
     * BCrypt 加密器：强度使用默认的 10 轮，兼顾安全与登录耗时
     */
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    /**
     * 历史版本使用的固定盐，仅用于兼容存量账号
     */
    private static final String LEGACY_SALT = "normal";

    /**
     * 对明文密码进行 BCrypt 加密
     *
     * @param rawPassword 明文密码
     * @return BCrypt 哈希
     */
    public String encode(String rawPassword) {
        if (StrUtil.isBlank(rawPassword)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return BCRYPT.encode(rawPassword);
    }

    /**
     * 校验明文密码与数据库中的哈希是否匹配。
     *
     * <p>BCrypt 哈希与旧版 MD5 哈希都支持；比较过程使用常量时间比较，避免时序攻击。</p>
     *
     * @param rawPassword    明文密码
     * @param storedPassword 数据库中的密码哈希
     * @return true 表示密码正确
     */
    public boolean matches(String rawPassword, String storedPassword) {
        if (StrUtil.isBlank(rawPassword) || StrUtil.isBlank(storedPassword)) {
            return false;
        }
        // 新版本：BCrypt 自带盐，直接交给加密器校验
        if (isBcrypt(storedPassword)) {
            return BCRYPT.matches(rawPassword, storedPassword);
        }
        // 旧版本：固定盐 + MD5，使用常量时间比较防止时序侧信道
        byte[] expected = legacyMd5(rawPassword).getBytes(StandardCharsets.UTF_8);
        byte[] actual = storedPassword.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    /**
     * 判断数据库中的密码哈希是否需要升级为 BCrypt
     *
     * @param storedPassword 数据库中的密码哈希
     * @return true 表示需要升级
     */
    public boolean needsUpgrade(String storedPassword) {
        return StrUtil.isBlank(storedPassword) || !isBcrypt(storedPassword);
    }

    /**
     * 判断一个哈希是否为 BCrypt 格式
     *
     * @param value 待判断的字符串
     * @return true 表示是 BCrypt 哈希
     */
    public boolean isBcrypt(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        // BCrypt 哈希以 $2a$ / $2b$ / $2y$ 开头，长度为 60
        return value.length() == 60
                && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    /**
     * 生成历史版本的「固定盐 + MD5」哈希，仅用于兼容存量账号
     *
     * @param rawPassword 明文密码
     * @return 32 位 MD5 字符串
     */
    public String legacyMd5(String rawPassword) {
        return DigestUtil.md5Hex(LEGACY_SALT + rawPassword);
    }
}
