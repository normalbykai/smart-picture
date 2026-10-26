package com.normal.picturebackend.infrastucture.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * 图片文件安全校验工具。
 *
 * <p>仅校验扩展名很容易被绕过（把任意文件改名成 .jpg 即可上传），
 * 因此这里同时校验：扩展名、文件头魔数、以及能否被 ImageIO 正常解码。</p>
 *
 * @author norma
 */
@Slf4j
public final class ImageFileValidator {

    /**
     * 允许的图片扩展名
     */
    private static final Set<String> ALLOWED_SUFFIXES = Set.of("jpg", "jpeg", "png", "webp");

    /**
     * 读取文件头时需要的字节数（WebP 需要读到第 12 个字节）
     */
    private static final int HEADER_LENGTH = 12;

    /**
     * 允许的最大图片边长，防止解码超大图片导致内存溢出
     */
    private static final int MAX_IMAGE_EDGE = 20000;

    /**
     * 工具类不允许实例化
     */
    private ImageFileValidator() {
    }

    /**
     * 校验 MultipartFile 类型的图片文件
     *
     * @param file 上传的文件
     */
    public static void validateMultipart(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String suffix = validateSuffix(originalFilename);
        try (InputStream inputStream = file.getInputStream()) {
            String format = detectFormat(inputStream);
            if (format == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容不是受支持的图片格式");
            }
            // 文件内容与扩展名必须匹配，防止「改后缀」绕过
            if (!suffixMatchesFormat(suffix, format)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容与扩展名不一致");
            }
            // WebP 在部分 JDK 上没有解码器，跳过解码校验，仅依赖魔数
            if (!"webp".equals(format)) {
                validateDecodable(file);
            }
        } catch (IOException e) {
            log.error("读取上传图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取图片失败");
        }
    }

    /**
     * 校验已经落盘的图片文件（主要用于 URL 上传场景）
     *
     * <p>URL 上传时扩展名可能缺失，因此这里只校验文件头与可解码性，不强制扩展名匹配。</p>
     *
     * @param file 已下载到本地的临时文件
     */
    public static void validateDownloadedFile(File file) {
        if (file == null || !file.exists() || file.length() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片文件为空");
        }
        String format;
        try (InputStream inputStream = new java.io.FileInputStream(file)) {
            format = detectFormat(inputStream);
        } catch (IOException e) {
            log.error("读取下载图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取图片失败");
        }
        if (format == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容不是受支持的图片格式");
        }
        // WebP 解码器可能缺失，跳过解码校验
        if ("webp".equals(format)) {
            return;
        }
        validateDecodable(file);
    }

    /**
     * 校验扩展名是否合法
     *
     * @param originalFilename 原始文件名
     * @return 小写扩展名
     */
    public static String validateSuffix(String originalFilename) {
        if (StrUtil.isBlank(originalFilename)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }
        String suffix = FileUtil.getSuffix(originalFilename);
        if (StrUtil.isBlank(suffix) || !ALLOWED_SUFFIXES.contains(suffix.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型不正确，仅支持 jpg/jpeg/png/webp");
        }
        return suffix.toLowerCase(Locale.ROOT);
    }

    /**
     * 通过文件头识别真实图片格式
     *
     * @param inputStream 输入流（方法内部只读取文件头，不关闭流）
     * @return jpeg / png / webp，无法识别时返回 null
     */
    private static String detectFormat(InputStream inputStream) {
        try {
            byte[] header = inputStream.readNBytes(HEADER_LENGTH);
            if (header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF) {
                return "jpeg";
            }
            if (header.length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                    && (header[4] & 0xFF) == 0x0D && (header[5] & 0xFF) == 0x0A
                    && (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A) {
                return "png";
            }
            if (header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return "webp";
            }
            return null;
        } catch (IOException e) {
            log.warn("读取图片文件头失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验扩展名与真实格式是否匹配
     *
     * @param suffix 扩展名
     * @param format 真实格式
     * @return true 表示匹配
     */
    private static boolean suffixMatchesFormat(String suffix, String format) {
        return switch (format) {
            case "jpeg" -> "jpg".equals(suffix) || "jpeg".equals(suffix);
            case "png" -> "png".equals(suffix);
            case "webp" -> "webp".equals(suffix);
            default -> false;
        };
    }

    /**
     * 校验图片能否被正常解码，并限制最大边长
     *
     * @param file 图片文件
     */
    private static void validateDecodable(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片文件无法解析");
            }
            if (image.getWidth() > MAX_IMAGE_EDGE || image.getHeight() > MAX_IMAGE_EDGE) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片尺寸过大");
            }
        } catch (IOException e) {
            log.error("解析图片失败", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片文件无法解析");
        }
    }

    /**
     * 校验 MultipartFile 能否被正常解码
     *
     * @param file 上传文件
     */
    private static void validateDecodable(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片文件无法解析");
            }
            if (image.getWidth() > MAX_IMAGE_EDGE || image.getHeight() > MAX_IMAGE_EDGE) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片尺寸过大");
            }
        } catch (IOException e) {
            log.error("解析上传图片失败", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片文件无法解析");
        }
    }
}
