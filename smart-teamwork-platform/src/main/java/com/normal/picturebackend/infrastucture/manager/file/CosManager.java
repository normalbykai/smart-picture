package com.normal.picturebackend.infrastucture.manager.file;


import cn.hutool.core.io.FileUtil;
import com.normal.picturebackend.infrastucture.config.CosClientConfig;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.DeleteObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest =
                new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(0);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        // 1. 构造上传对象请求
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);

        // 2. 对图片进行处理（获取基本信息也被视作为一种图片的处理）
        PicOperations picOperations = new PicOperations();
        // 0:不返回原图信息 - 1:返回原图信息, 默认为0
        picOperations.setIsPicInfo(1);

        // 3. 构建图片处理规则
        List<PicOperations.Rule> rules = getRules(key, file);
        // 添加图片处理规则
        picOperations.setRules(rules);

        // 构造处理参数
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 图片处理规则
     * - 图片压缩
     * - 图片缩略图
     *
     * @param key
     * @param file
     * @return
     */
    private List<PicOperations.Rule> getRules(String key, File file) {
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 1. 添加图片压缩格式规则 (转换为web格式)
        // 注意：必须基于对象键生成，保证压缩图与缩略图和原图在同一目录下
        String mainName = removeExtension(key);
        String webpKey = mainName + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);

        // 2. 添加缩略图规则(图片大小大于20KB才进行缩略图)
        if (file.length() > 20 * 1024) {
            PicOperations.Rule thumbnail = new PicOperations.Rule();
            thumbnail.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 512, 512));
            thumbnail.setBucket(cosClientConfig.getBucket());
            String thumbnailKey = mainName + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnail.setFileId(thumbnailKey);
            rules.add(thumbnail);
        }

        // 返回规则集合
        return rules;
    }

    /**
     * 去掉对象键的扩展名，同时保留目录层级
     * 例如：public/1/a.jpg -> public/1/a
     */
    private String removeExtension(String key) {
        int slashIndex = key.lastIndexOf('/');
        int dotIndex = key.lastIndexOf('.');
        if (dotIndex > slashIndex) {
            return key.substring(0, dotIndex);
        }
        return key;
    }

    /**
     * 检查桶是否存在（用于切换存储时探测连通性）
     */
    public boolean doesBucketExist() {
        return cosClient.doesBucketExist(cosClientConfig.getBucket());
    }

    /**
     * 通过URL地址删除COS中图片
     */
    public void deletePicture(String key) {
        try {
            cosClient.deleteObject(cosClientConfig.getBucket(), key);
        } catch (Exception e) {
            log.error("删除对象存储图片失败:key:{}", key);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除对象存储图片失败");
        }
    }

}

