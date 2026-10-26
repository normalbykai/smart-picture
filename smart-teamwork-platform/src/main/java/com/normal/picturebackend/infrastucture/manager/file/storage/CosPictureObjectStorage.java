package com.normal.picturebackend.infrastucture.manager.file.storage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.normal.picturebackend.infrastucture.config.CosClientConfig;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.normal.picturebackend.infrastucture.manager.file.CosManager;
import com.normal.picturebackend.infrastucture.manager.file.model.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 腾讯云 COS 图片存储实现
 * <p>
 * 继续使用数据万象完成图片压缩、缩略图和图片信息获取。
 */
@Slf4j
@Component
public class CosPictureObjectStorage implements PictureObjectStorage {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    @Override
    public StorageTypeEnum getType() {
        return StorageTypeEnum.COS;
    }

    @Override
    public boolean isConfigured() {
        return StrUtil.isNotBlank(cosClientConfig.getHost())
                && StrUtil.isNotBlank(cosClientConfig.getSecretId())
                && StrUtil.isNotBlank(cosClientConfig.getSecretKey())
                && StrUtil.isNotBlank(cosClientConfig.getBucket());
    }

    @Override
    public boolean testConnection() {
        if (!isConfigured()) {
            return false;
        }
        try {
            return cosManager.doesBucketExist();
        } catch (Exception e) {
            log.warn("COS 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public UploadPictureResult uploadPicture(String key, File file) {
        PutObjectResult putObjectResult;
        try {
            putObjectResult = cosManager.putPictureObject(key, file);
        } catch (Exception e) {
            log.error("COS 上传图片失败: key={}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "COS 上传图片失败: " + e.getMessage());
        }
        CIUploadResult ciUploadResult = putObjectResult.getCiUploadResult();
        if (ciUploadResult == null || ciUploadResult.getOriginalInfo() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "COS 上传成功但未返回图片信息");
        }

        UploadPictureResult result = new UploadPictureResult();
        result.setOriginUrl(getUrl(key));
        result.setUrl(getUrl(key));
        result.setThumbnailUrl(getUrl(key));
        result.setPicSize(file.length());

        // 1. 原图信息
        ImageInfo imageInfo = ciUploadResult.getOriginalInfo().getImageInfo();
        if (imageInfo != null) {
            fillPictureInfo(result, imageInfo.getWidth(), imageInfo.getHeight(), imageInfo.getFormat());
            result.setMainColor(imageInfo.getAve());
        }

        // 2. 数据万象处理结果：第一项为 webp 压缩图，第二项为缩略图（可选）
        ProcessResults processResults = ciUploadResult.getProcessResults();
        if (processResults == null) {
            return result;
        }
        List<CIObject> objectList = processResults.getObjectList();
        if (CollUtil.isEmpty(objectList)) {
            return result;
        }

        CIObject compressObject = objectList.get(0);
        if (compressObject != null) {
            if (StrUtil.isNotBlank(compressObject.getKey())) {
                result.setUrl(getUrl(compressObject.getKey()));
            }
            if (compressObject.getSize() != null) {
                result.setPicSize(compressObject.getSize().longValue());
            }
            fillPictureInfo(result, compressObject.getWidth(), compressObject.getHeight(), compressObject.getFormat());
        }

        CIObject thumbnailObject = objectList.size() > 1 ? objectList.get(1) : compressObject;
        if (thumbnailObject != null && StrUtil.isNotBlank(thumbnailObject.getKey())) {
            result.setThumbnailUrl(getUrl(thumbnailObject.getKey()));
        }
        return result;
    }

    @Override
    public String uploadFile(String key, File file) {
        try {
            cosManager.putObject(key, file);
        } catch (Exception e) {
            log.error("COS 上传文件失败: key={}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "COS 上传文件失败: " + e.getMessage());
        }
        return getUrl(key);
    }

    @Override
    public void delete(String key) {
        cosManager.deletePicture(key);
    }

    @Override
    public String getUrl(String key) {
        String host = StrUtil.removeSuffix(StrUtil.nullToEmpty(cosClientConfig.getHost()), "/");
        return host + "/" + StrUtil.removePrefix(key, "/");
    }

    @Override
    public String getKey(String url) {
        if (StrUtil.isBlank(url)) {
            return "";
        }
        String host = StrUtil.removeSuffix(StrUtil.nullToEmpty(cosClientConfig.getHost()), "/");
        if (StrUtil.isNotBlank(host) && url.startsWith(host)) {
            return StrUtil.removePrefix(url.substring(host.length()), "/");
        }
        try {
            String path = URI.create(url).getPath();
            return URLDecoder.decode(StrUtil.removePrefix(path, "/"), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("解析 COS 图片地址失败: {}", url);
            return StrUtil.removePrefix(url, "/");
        }
    }

    @Override
    public boolean matches(String url) {
        String host = StrUtil.removeSuffix(StrUtil.nullToEmpty(cosClientConfig.getHost()), "/");
        return StrUtil.isNotBlank(host) && StrUtil.isNotBlank(url) && url.startsWith(host + "/");
    }

    /**
     * 填充图片宽高、比例、格式
     */
    private void fillPictureInfo(UploadPictureResult result, Integer width, Integer height, String format) {
        if (width != null && width > 0) {
            result.setPicWidth(width);
        }
        if (height != null && height > 0) {
            result.setPicHeight(height);
        }
        if (result.getPicHeight() > 0) {
            result.setPicScale(NumberUtil.round(result.getPicWidth() * 1.0 / result.getPicHeight(), 2).doubleValue());
        }
        if (StrUtil.isNotBlank(format)) {
            result.setPicFormat(format);
        }
    }
}
