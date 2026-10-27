package com.normal.picturebackend.infrastructure.manager.file;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.normal.picturebackend.infrastructure.config.CosClientConfig;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.infrastructure.exception.ThrowUtils;
import com.normal.picturebackend.infrastructure.manager.file.model.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 【已废弃】
 * - 建议使用 file包下
 *
 * 文件通用操作
 *
 *
 * @author: normal
 * @description:
 */
@Component
@Slf4j
@Deprecated
public class FileManager {

    @Resource
    CosManager cosManager;

    @Resource
    CosClientConfig cosClientConfig;

    // 1M 大小
    public static final long ONE_M = 1024 * 1024;

    // 允许上传文件的后缀名
    public static final List<String> ALLOW_FORMAT_FILE = Arrays.asList("jpeg", "jpg", "png", "webp");


    /**
     * 上传图片文件
     *
     * @param file             文件
     * @param uploadPathPrefix 上传文件路径前缀
     * @return 文件上传
     */
    public UploadPictureResult uploadPictureFile(MultipartFile file, String uploadPathPrefix) {

        // 校验图片信息
        validPicture(file);

        // 图片上传地址
        String uploadPath = generateImageUploadPath(file, uploadPathPrefix);
        File tempFile = null;
        try {
            tempFile = File.createTempFile(uploadPath, null);
            file.transferTo(tempFile);
            // 上传到COS库
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            ImageInfo pictureInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 拿到数据万象处理后的图片数据 webp - 缩略图
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();


            int pictureInfoHeight = pictureInfo.getHeight();
            int pictureInfoWidth = pictureInfo.getWidth();
            // 图片宽高比
            double scale = NumberUtil.round((pictureInfoWidth * 1.0 / pictureInfoHeight), 2).doubleValue();
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(file.getOriginalFilename()));
            uploadPictureResult.setPicSize(file.getSize());
            uploadPictureResult.setPicWidth(pictureInfoWidth);
            uploadPictureResult.setPicHeight(pictureInfoHeight);
            uploadPictureResult.setPicScale(scale);
            uploadPictureResult.setPicFormat(pictureInfo.getFormat());

            return uploadPictureResult;
        } catch (Exception e) {
            log.error("fileUploadError: ${}", ExceptionUtil.getRootCauseMessage(e));
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            // 清除临时文件
            clearTempFile(tempFile);
        }
    }



    /**
     * 生产图片路径
     *
     * @param file 文件
     * @param uploadPathPrefix 文件前缀路径
     * @return 图片路径
     */
    private String generateImageUploadPath(MultipartFile file, String uploadPathPrefix) {
        // 文件原始地址
        String originalFilename = file.getOriginalFilename();
        String uuid = RandomUtil.randomString(16);
        String customFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        return String.format("%s%s", uploadPathPrefix, customFileName);
    }

    /**
     * 清空临时文件
     * @param tempFile 临时文件
     */
    private void clearTempFile(File tempFile) {
        if (tempFile == null) {
            return;
        }
        boolean delete = tempFile.delete();
        if (!delete) {
            log.error("file delete error, filepath = ${}", tempFile.getAbsoluteFile());
        }
    }

    private void validPicture(MultipartFile file) {
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 1. 文件大小校验
        long size = file.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(size >= 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");

        // 2. 文件后缀名校验
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        // 校验允许上传文件
        ThrowUtils.throwIf(!ALLOW_FORMAT_FILE.contains(suffix), ErrorCode.PARAMS_ERROR, "文件类型不正确");


    }
}
