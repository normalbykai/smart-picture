package com.normal.picturebackend.infrastructure.manager.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.infrastructure.exception.ThrowUtils;
import com.normal.picturebackend.infrastructure.utils.ImageFileValidator;
import com.normal.picturebackend.infrastructure.utils.UrlSecurityValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.Set;

/**
 * 通过 URL 地址上传图片。
 *
 * <p>安全要点：</p>
 * <ol>
 *     <li>只允许 http/https，并校验域名解析结果，禁止访问内网/回环/云元数据地址（防 SSRF）；</li>
 *     <li>手动处理重定向，每一跳都重新校验，最多 3 次；</li>
 *     <li>下载过程按 2MB 截断，避免超大文件打满磁盘；</li>
 *     <li>下载完成后校验文件头与可解码性，避免把任意文件当图片上传。</li>
 * </ol>
 */
@Component
@Slf4j
public class PictureUrlManager extends PictureUploadTemplate {

    /**
     * 允许的文件后缀
     */
    private static final Set<String> ALLOW_SUFFIXES = Set.of("jpg", "jpeg", "png", "webp");

    /**
     * 限制文件最大大小 2MB
     */
    private static final long ONE_M = 1024 * 1024;
    private static final long FILE_MAX_SIZE = 2 * ONE_M;

    /**
     * 最大重定向次数
     */
    private static final int MAX_REDIRECTS = 3;

    /**
     * 下载超时时间（毫秒）
     */
    private static final int DOWNLOAD_TIMEOUT_MS = 10_000;

    /**
     * URL 地址校验：只做协议与内网地址校验，文件类型在下载完成后校验
     *
     * @param inputSource 输入源
     */
    @Override
    public void validPicture(Object inputSource) {
        ThrowUtils.throwIf(!(inputSource instanceof String), ErrorCode.PARAMS_ERROR, "url地址不能为空");
        String url = (String) inputSource;
        UrlSecurityValidator.validateAndParse(url);
    }

    /**
     * 通过 URL 地址获取文件名
     *
     * @param inputSource 输入源
     * @return 文件名
     */
    @Override
    public String getOriginFileName(Object inputSource) {
        String url = (String) inputSource;
        String originFileName = "";
        try {
            // 先取 URI 的 path，避免把 ?query 当成文件名的一部分
            String path = URI.create(url).getPath();
            originFileName = FileUtil.getName(path);
        } catch (Exception e) {
            log.warn("解析 URL 文件名失败，使用默认文件名: {}", url);
        }
        if (StrUtil.isBlank(originFileName)) {
            originFileName = "image";
        }
        String suffix = StrUtil.nullToEmpty(FileUtil.getSuffix(originFileName)).toLowerCase(Locale.ROOT);
        if (!ALLOW_SUFFIXES.contains(suffix)) {
            // 无法从 URL 判断类型时先按 jpg 处理，真实类型由文件头校验兜底
            originFileName = FileUtil.mainName(originFileName) + ".jpg";
        }
        return originFileName;
    }

    /**
     * 安全下载 URL 内容到本地临时文件
     *
     * @param inputSource 输入源
     * @param tempFile    本地临时文件
     * @throws IOException 输入输出异常
     */
    @Override
    public void processFile(Object inputSource, File tempFile) throws IOException {
        URL currentUrl = UrlSecurityValidator.validateAndParse((String) inputSource);
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            HttpRequest request = HttpUtil.createRequest(Method.GET, currentUrl.toString())
                    .setFollowRedirects(false)
                    .timeout(DOWNLOAD_TIMEOUT_MS);
            try (HttpResponse response = request.execute()) {
                int status = response.getStatus();
                // 1. 处理重定向：每一跳都重新做 URL 安全校验
                if (status >= 300 && status < 400) {
                    String location = response.header("Location");
                    currentUrl = UrlSecurityValidator.resolveRedirect(currentUrl, location);
                    continue;
                }
                if (status != HttpStatus.HTTP_OK) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "下载图片失败，状态码: " + status);
                }
                // 2. 内容类型与大小预校验
                validateContentType(response.header("Content-Type"));
                validateContentLength(response.header("Content-Length"));
                // 3. 流式下载并按大小截断，避免服务端不返回 Content-Length 时被撑爆
                copyWithLimit(response, tempFile);
            }
            // 4. 下载完成后做文件头与解码校验，失败会抛业务异常
            ImageFileValidator.validateDownloadedFile(tempFile);
            return;
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "重定向次数过多");
    }

    /**
     * 校验响应内容类型
     *
     * @param contentType 响应头中的 Content-Type
     */
    private void validateContentType(String contentType) {
        if (StrUtil.isBlank(contentType)) {
            // 部分服务端不返回类型，交给文件头校验兜底
            return;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        // 允许 image/* 与常见的二进制流类型，最终仍以文件头为准
        if (!lower.startsWith("image/") && !lower.startsWith("application/octet-stream")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该文件的类型不支持");
        }
    }

    /**
     * 校验响应声明的文件大小
     *
     * @param contentLength 响应头中的 Content-Length
     */
    private void validateContentLength(String contentLength) {
        if (StrUtil.isBlank(contentLength)) {
            return;
        }
        try {
            long size = Long.parseLong(contentLength);
            ThrowUtils.throwIf(size > FILE_MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式不正确");
        }
    }

    /**
     * 流式复制响应体，并在超过 2MB 时立即中断
     *
     * @param response 响应
     * @param tempFile 临时文件
     * @throws IOException 输入输出异常
     */
    private void copyWithLimit(HttpResponse response, File tempFile) throws IOException {
        InputStream inputStream = response.bodyStream();
        if (inputStream == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "下载图片失败，响应体为空");
        }
        try (InputStream in = inputStream; OutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int len;
            while ((len = in.read(buffer)) != -1) {
                total += len;
                if (total > FILE_MAX_SIZE) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
                }
                out.write(buffer, 0, len);
            }
            if (total == 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "下载图片失败，文件为空");
            }
        }
    }
}
