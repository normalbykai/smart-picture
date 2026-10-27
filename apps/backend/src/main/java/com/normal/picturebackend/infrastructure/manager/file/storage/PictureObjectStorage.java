package com.normal.picturebackend.infrastructure.manager.file.storage;

import com.normal.picturebackend.infrastructure.manager.file.model.UploadPictureResult;

import java.io.File;

/**
 * 图片对象存储抽象
 * <p>
 * COS 与 MinIO 各自实现该接口，上传模板只依赖此接口，
 * 从而可以在不修改业务代码的情况下切换底层存储。
 */
public interface PictureObjectStorage {

    /**
     * 存储类型
     */
    StorageTypeEnum getType();

    /**
     * 配置是否完整（不发起网络请求）
     */
    boolean isConfigured();

    /**
     * 测试存储是否可用（会发起网络请求）
     */
    boolean testConnection();

    /**
     * 上传图片，并在存储端完成图片处理（图片信息、缩略图等）
     *
     * @param key  对象键，如 public/1/2025-01-01_xxx.jpg
     * @param file 本地文件
     * @return 上传结果（不含图片名称）
     */
    UploadPictureResult uploadPicture(String key, File file);

    /**
     * 上传文件（不做图片处理）
     *
     * @param key  对象键
     * @param file 本地文件
     * @return 可直接访问的 URL
     */
    String uploadFile(String key, File file);

    /**
     * 删除对象
     *
     * @param key 对象键
     */
    void delete(String key);

    /**
     * 对象键转访问 URL
     */
    String getUrl(String key);

    /**
     * 访问 URL 转对象键（用于删除历史图片）
     */
    String getKey(String url);

    /**
     * 判断 URL 是否属于当前存储（用于按 URL 定位历史图片所在存储）
     */
    boolean matches(String url);
}
