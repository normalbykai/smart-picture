package com.normal.picturebackend.infrastucture.manager.file;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.normal.picturebackend.infrastucture.manager.file.model.UploadPictureResult;
import com.normal.picturebackend.infrastucture.manager.file.storage.PictureObjectStorage;
import com.normal.picturebackend.infrastucture.manager.file.storage.PictureStorageRouter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * 图片文件上传 模板方法
 * <p>
 * 模板只负责校验、生成对象键、准备本地临时文件，
 * 真正的上传与图片处理交给当前选中的对象存储（COS / MinIO）。
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private PictureStorageRouter pictureStorageRouter;

    /**
     * 上传图片文件 （图片基本参数信息）
     *
     * @param inputSource      传入资源
     * @param uploadPathPrefix 上传文件路径前缀
     * @return 文件信息包装类
     */
    public UploadPictureResult uploadPictureFile(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片信息（抽象方法需要外部继承并实现该抽象接口）
        validPicture(inputSource);

        // 2. 构建图片上传地址
        String originFileName = getOriginFileName(inputSource);
        String uploadPath = this.generatedUploadPath(originFileName, uploadPathPrefix);

        // 3. 将图片保存到本地 temp 中，然后上传到当前对象存储
        File localTempFile = null;
        try {
            localTempFile = createTempFile(originFileName);
            this.processFile(inputSource, localTempFile);

            // 4. 上传到当前选中的对象存储（COS 或 MinIO），并完成图片处理
            UploadPictureResult uploadPictureResult = pictureStorageRouter.current()
                    .uploadPicture(uploadPath, localTempFile);
            uploadPictureResult.setPicName(FileUtil.mainName(originFileName));
            return uploadPictureResult;
        } catch (BusinessException e) {
            log.error("图片上传失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("fileUploadError: {}", ExceptionUtil.getRootCauseMessage(e));
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            // 5. 清除临时文件
            clearTempFile(localTempFile);
        }
    }

    /**
     * 上传图片文件 (获取上传图片后路径)
     *
     * @param inputSource      传入资源
     * @param uploadPathPrefix 上传文件路径前缀
     * @return 图片上传后地址
     */
    public String uploadPictureAndGetPath(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片信息（抽象方法需要外部继承并实现该抽象接口）
        validPicture(inputSource);

        // 2. 构建图片上传地址
        String originFileName = getOriginFileName(inputSource);
        String uploadPath = this.generatedUploadPath(originFileName, uploadPathPrefix);

        // 3. 开始上传图片
        File localTempFile = null;
        try {
            localTempFile = createTempFile(originFileName);
            this.processFile(inputSource, localTempFile);

            // 4. 上传到当前选中的对象存储
            return pictureStorageRouter.current().uploadFile(uploadPath, localTempFile);
        } catch (BusinessException e) {
            log.error("图片上传失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("fileUploadError: {}", ExceptionUtil.getRootCauseMessage(e));
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            // 5. 清除临时文件
            clearTempFile(localTempFile);
        }
    }

    /**
     * 删除对象存储图片
     * <p>
     * 根据图片 URL 自动定位到它所在的存储（COS / MinIO），
     * 因此切换存储后仍然可以删除历史图片。
     *
     * @param url 图片地址
     * @return 删除结果
     */
    public boolean deletePicture(String url) {
        // 1. 参数校验
        if (StrUtil.isBlank(url)) {
            // 无需删除
            return false;
        }

        // 2. 定位图片所在存储并解析对象键
        PictureObjectStorage storage = pictureStorageRouter.resolveByUrlOrNull(url);
        if (storage == null) {
            // 无法判断归属时跳过，避免把对象键错误地删到当前存储上
            log.warn("无法识别图片所属存储，跳过删除: {}", url);
            return false;
        }
        String key = storage.getKey(url);
        if (StrUtil.isBlank(key)) {
            log.warn("无法从图片地址中解析对象键，跳过删除: {}", url);
            return false;
        }

        // 3. 执行删除逻辑
        storage.delete(key);
        return true;
    }

    /**
     * 生成对象存储路径
     * 例如: public/123/2024-12-21_abcdef.jpg
     *
     * @param originFileName   图片原始文件名
     * @param uploadPathPrefix 存储路径前缀(如: "public/123")
     * @return 对象键
     */
    private String generatedUploadPath(String originFileName, String uploadPathPrefix) {
        // 1. 重构图片文件名 (年月日_唯一标识UUID_后缀名)
        String uploadData = DateUtil.formatDate(new Date());
        String uuid = RandomUtil.randomString(16);
        String suffix = FileUtil.getSuffix(originFileName);
        String pictureName = String.format("%s_%s.%s", uploadData, uuid, suffix);

        // 2. 格式化上传路径（统一处理多余的 /，避免出现 //）
        String prefix = normalizePrefix(uploadPathPrefix);
        return StrUtil.isBlank(prefix) ? pictureName : String.format("%s/%s", prefix, pictureName);
    }

    /**
     * 创建带正确后缀的临时文件，保证 COS/MinIO 能识别文件类型
     */
    private File createTempFile(String originFileName) throws IOException {
        String suffix = FileUtil.getSuffix(originFileName);
        String tempSuffix = StrUtil.isBlank(suffix) ? ".tmp" : "." + suffix;
        return File.createTempFile("picture-upload-", tempSuffix);
    }

    /**
     * 去掉路径前缀首尾的 /
     */
    private String normalizePrefix(String uploadPathPrefix) {
        String prefix = StrUtil.nullToEmpty(uploadPathPrefix).trim().replace('\\', '/');
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    /**
     * 清空临时文件
     *
     * @param tempFile 临时文件
     */
    private void clearTempFile(File tempFile) {
        if (tempFile == null) {
            return;
        }
        boolean delete = tempFile.delete();
        if (!delete) {
            log.error("file delete error, filepath = {}", tempFile.getAbsoluteFile());
        }
    }

    /**
     * 文件上传资源校验
     *
     * @param inputSource 输入源
     */
    public abstract void validPicture(Object inputSource);

    /**
     * 获取文件原始名字 如 normal.jpeg | admin.png | picture.jpg
     *
     * @param inputSource 输入源
     * @return 文件路径
     */
    public abstract String getOriginFileName(Object inputSource);

    /**
     * 将输入源文件 保存到本地tempFile文件中
     *
     * @param inputSource 输入源
     * @param tempFile    本地临时文件
     * @throws IOException 输入输出异常
     */
    public abstract void processFile(Object inputSource, File tempFile) throws IOException;
}
