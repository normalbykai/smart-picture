package com.normal.picturebackend.infrastucture.manager.file;


import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.normal.picturebackend.infrastucture.exception.ThrowUtils;
import com.normal.picturebackend.infrastucture.utils.ImageFileValidator;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 图片文件上传。
 *
 * <p>负责校验上传文件、生成临时文件并交给模板方法上传。
 * 安全校验统一收敛到 {@link ImageFileValidator}：扩展名、文件头魔数、可解码性三重校验。</p>
 */
@Component
public class PictureFileManager extends PictureUploadTemplate {

    /**
     * 允许上传文件的后缀名（保留该常量，方便其他模块复用）
     */
    public static final List<String> ALLOW_FORMAT_FILE = List.of("jpeg", "jpg", "png", "webp");

    /**
     * 1M 大小
     */
    public static final long ONE_M = 1024 * 1024;

    /**
     * 单张图片最大 2MB
     */
    private static final long FILE_MAX_SIZE = 2 * ONE_M;

    /**
     * 图片文件 - 参数校验
     *
     * @param inputSource 输入源 (MultipartFile类型)
     */
    @Override
    public void validPicture(Object inputSource) {
        ThrowUtils.throwIf(!(inputSource instanceof MultipartFile), ErrorCode.PARAMS_ERROR, "文件不能为空");
        MultipartFile file = (MultipartFile) inputSource;

        // 1. 文件大小校验：防止超大文件占用磁盘与内存
        ThrowUtils.throwIf(file.getSize() > FILE_MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");

        // 2. 文件内容校验：扩展名 + 文件头 + 可解码性
        ImageFileValidator.validateMultipart(file);
    }

    /**
     * 获取原始文件名
     *
     * @param inputSource 输入源
     * @return 文件原始文件名
     */
    @Override
    public String getOriginFileName(Object inputSource) {
        MultipartFile file = (MultipartFile) inputSource;
        return file.getOriginalFilename();
    }

    /**
     * 将输入源的文件写入本地临时文件
     *
     * @param inputSource 输入源
     * @param tempFile    临时文件
     * @throws IOException 输入输出异常
     */
    @Override
    public void processFile(Object inputSource, File tempFile) throws IOException {
        MultipartFile file = (MultipartFile) inputSource;
        file.transferTo(tempFile);
    }
}
