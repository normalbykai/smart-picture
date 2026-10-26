package com.normal.picturebackend.infrastucture.manager.file.storage;

import cn.hutool.core.util.StrUtil;
import com.normal.picturebackend.infrastucture.config.StorageProperties;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 对象存储路由
 * <p>
 * 负责在 COS / MinIO 之间选择当前使用的存储。
 * 既支持通过 {@code storage.type} 配置启动时选择，
 * 也支持通过管理接口在运行时切换（重启后恢复为配置值）。
 */
@Slf4j
@Component
public class PictureStorageRouter {

    private final Map<StorageTypeEnum, PictureObjectStorage> storageMap;

    private final AtomicReference<StorageTypeEnum> currentType;

    public PictureStorageRouter(List<PictureObjectStorage> storages, StorageProperties storageProperties) {
        Map<StorageTypeEnum, PictureObjectStorage> map = new EnumMap<>(StorageTypeEnum.class);
        for (PictureObjectStorage storage : storages) {
            map.put(storage.getType(), storage);
        }
        this.storageMap = Collections.unmodifiableMap(map);

        StorageTypeEnum configured = StorageTypeEnum.from(storageProperties.getType());
        if (configured == null) {
            throw new IllegalStateException("storage.type 配置不正确，可选值：cos、minio，当前值：" + storageProperties.getType());
        }
        if (!storageMap.containsKey(configured)) {
            throw new IllegalStateException("未找到对象存储实现：" + configured.getValue());
        }
        this.currentType = new AtomicReference<>(configured);
        log.info("当前对象存储：{}（{}）", configured.getLabel(), configured.getValue());
    }

    /**
     * 当前使用的存储
     */
    public PictureObjectStorage current() {
        PictureObjectStorage storage = storageMap.get(currentType.get());
        if (storage == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "对象存储未初始化");
        }
        return storage;
    }

    public StorageTypeEnum getCurrentType() {
        return currentType.get();
    }

    /**
     * 运行时切换存储
     *
     * @param type 目标存储类型
     */
    public PictureObjectStorage switchTo(StorageTypeEnum type) {
        if (type == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "存储类型不能为空");
        }
        PictureObjectStorage storage = storageMap.get(type);
        if (storage == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的对象存储类型：" + type.getValue());
        }
        if (!storage.isConfigured()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, type.getLabel() + " 配置不完整，无法切换");
        }
        // 连接测试只作为提示，不阻断切换：
        // 例如 COS 子账号可能没有 HeadBucket 权限，但 PutObject 权限正常
        if (!storage.testConnection()) {
            log.warn("{} 连接测试未通过，仍允许切换；上传时如失败请检查配置或服务状态", type.getLabel());
        }
        StorageTypeEnum old = currentType.getAndSet(type);
        log.info("对象存储已切换：{} -> {}", old.getValue(), type.getValue());
        return storage;
    }

    /**
     * 根据历史图片 URL 定位其所在存储。
     *
     * <p>与 {@link #resolveByUrl(String)} 不同，这里找不到匹配项时返回 null，
     * 用于删除场景：宁可跳过，也不要把对象键错误地删到当前存储上。</p>
     *
     * @param url 图片地址
     * @return 匹配的存储，未匹配返回 null
     */
    public PictureObjectStorage resolveByUrlOrNull(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        for (PictureObjectStorage storage : storageMap.values()) {
            if (storage.matches(url)) {
                return storage;
            }
        }
        return null;
    }

    /**
     * 根据历史图片 URL 定位其所在存储（找不到时回退到当前存储）
     *
     * @param url 图片地址
     * @return 存储实现
     */
    public PictureObjectStorage resolveByUrl(String url) {
        PictureObjectStorage storage = resolveByUrlOrNull(url);
        return storage != null ? storage : current();
    }

    public PictureObjectStorage get(StorageTypeEnum type) {
        return storageMap.get(type);
    }

    public List<PictureObjectStorage> listAll() {
        return new ArrayList<>(storageMap.values());
    }
}
