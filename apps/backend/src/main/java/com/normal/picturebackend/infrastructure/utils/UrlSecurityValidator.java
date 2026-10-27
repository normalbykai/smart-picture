package com.normal.picturebackend.infrastructure.utils;

import cn.hutool.core.util.StrUtil;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * 外部 URL 安全校验工具（防 SSRF）。
 *
 * <p>URL 上传场景下，服务端会主动访问用户提供的地址。如果不做限制，
 * 攻击者可以借服务端访问内网服务、云厂商元数据地址（169.254.169.254）等。
 * 本工具在每次请求（包括重定向）前校验协议、域名解析结果，禁止访问内网地址。</p>
 *
 * <p>注意：DNS 解析与实际连接之间仍存在 rebinding 的理论窗口，
 * 生产环境建议再配合出口网络策略（如仅允许访问公网 80/443）。</p>
 *
 * @author norma
 */
@Slf4j
public final class UrlSecurityValidator {

    /**
     * 工具类不允许实例化
     */
    private UrlSecurityValidator() {
    }

    /**
     * 校验 URL 并解析为 {@link URL} 对象
     *
     * @param url 待校验的地址
     * @return 校验通过的 URL
     */
    public static URL validateAndParse(String url) {
        if (StrUtil.isBlank(url)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL 地址不能为空");
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL 格式不正确");
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只允许 http/https 协议");
        }
        String host = uri.getHost();
        if (StrUtil.isBlank(host)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL 缺少主机名");
        }
        // 校验域名解析出的所有 IP，任意一个命中内网即拒绝
        assertHostAllowed(host);
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL 格式不正确");
        }
    }

    /**
     * 解析重定向地址，并对新地址重新做安全校验
     *
     * @param current  当前地址
     * @param location 响应头中的 Location
     * @return 校验通过的新地址
     */
    public static URL resolveRedirect(URL current, String location) {
        if (StrUtil.isBlank(location)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "重定向地址为空");
        }
        try {
            // 使用 URI.resolve 兼容相对路径重定向（如 /a/b.jpg）
            URI target = current.toURI().resolve(location.trim());
            return validateAndParse(target.toString());
        } catch (URISyntaxException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "重定向地址不合法");
        }
    }

    /**
     * 校验主机名解析出的地址是否全部为公网地址
     *
     * @param host 主机名
     */
    private static void assertHostAllowed(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "域名无法解析");
        }
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                log.warn("拒绝访问内网地址: host={}, ip={}", host, address.getHostAddress());
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不允许访问内网地址");
            }
        }
    }

    /**
     * 判断一个 IP 是否属于禁止访问的范围
     *
     * @param address 解析后的地址
     * @return true 表示禁止访问
     */
    public static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return isBlockedIpv4(bytes);
        }
        if (bytes.length == 16) {
            // IPv6 唯一本地地址 fc00::/7
            return (bytes[0] & 0xFE) == 0xFC;
        }
        return true;
    }

    /**
     * 判断 IPv4 是否属于保留/内网网段
     *
     * @param bytes 4 字节地址
     * @return true 表示禁止访问
     */
    private static boolean isBlockedIpv4(byte[] bytes) {
        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;
        // 0.0.0.0/8、10.0.0.0/8、127.0.0.0/8、224.0.0.0/4（组播）、240.0.0.0/4（保留）
        if (first == 0 || first == 10 || first == 127 || first >= 224) {
            return true;
        }
        // 100.64.0.0/10 运营商级 NAT
        if (first == 100 && second >= 64 && second <= 127) {
            return true;
        }
        // 169.254.0.0/16 链路本地（含云厂商元数据地址）
        if (first == 169 && second == 254) {
            return true;
        }
        // 172.16.0.0/12
        if (first == 172 && second >= 16 && second <= 31) {
            return true;
        }
        // 192.168.0.0/16
        if (first == 192 && second == 168) {
            return true;
        }
        // 198.18.0.0/15 基准测试网段
        if (first == 198 && (second == 18 || second == 19)) {
            return true;
        }
        // 192.0.0.0/24 协议保留
        return first == 192 && second == 0 && (bytes[2] & 0xFF) == 0;
    }
}
