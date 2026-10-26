package com.normal.picturebackend.interfaces.controller;

import cn.hutool.core.util.StrUtil;
import com.normal.picturebackend.domain.user.valueobject.UserConstant;
import com.normal.picturebackend.infrastucture.annotation.AuthCheck;
import com.normal.picturebackend.infrastucture.common.BaseResponse;
import com.normal.picturebackend.infrastucture.common.ResultUtils;
import com.normal.picturebackend.infrastucture.config.CosClientConfig;
import com.normal.picturebackend.infrastucture.config.StorageProperties;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.normal.picturebackend.infrastucture.exception.ThrowUtils;
import com.normal.picturebackend.infrastucture.manager.file.storage.PictureObjectStorage;
import com.normal.picturebackend.infrastucture.manager.file.storage.PictureStorageRouter;
import com.normal.picturebackend.infrastucture.manager.file.storage.StorageTypeEnum;
import com.normal.picturebackend.interfaces.dto.storage.StorageSwitchRequest;
import com.normal.picturebackend.interfaces.vo.storage.StorageConfigVO;
import com.normal.picturebackend.interfaces.vo.storage.StorageInfoVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 对象存储管理接口（仅管理员）
 * <p>
 * 支持查看当前存储、运行时切换 COS / MinIO。
 * 切换只影响之后新上传的图片，历史图片仍使用数据库中保存的绝对 URL。
 */
@Slf4j
@RestController
@RequestMapping("/storage")
public class StorageController {

    @Resource
    private PictureStorageRouter pictureStorageRouter;

    @Resource
    private StorageProperties storageProperties;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 查看当前对象存储配置
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/config")
    public BaseResponse<StorageConfigVO> getStorageConfig() {
        return ResultUtils.success(buildStorageConfig());
    }

    /**
     * 切换对象存储
     *
     * @param request 目标存储类型
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/switch")
    public BaseResponse<StorageConfigVO> switchStorage(@Valid @RequestBody StorageSwitchRequest request) {
        ThrowUtils.throwIf(request == null || StrUtil.isBlank(request.getType()),
                ErrorCode.PARAMS_ERROR, "存储类型不能为空");
        StorageTypeEnum targetType = StorageTypeEnum.from(request.getType());
        ThrowUtils.throwIf(targetType == null, ErrorCode.PARAMS_ERROR,
                "不支持的对象存储类型，可选值：cos、minio");

        pictureStorageRouter.switchTo(targetType);
        return ResultUtils.success(buildStorageConfig());
    }

    private StorageConfigVO buildStorageConfig() {
        StorageTypeEnum currentType = pictureStorageRouter.getCurrentType();
        List<StorageInfoVO> storageInfoList = new ArrayList<>();
        for (PictureObjectStorage storage : pictureStorageRouter.listAll()) {
            StorageInfoVO.StorageInfoVOBuilder builder = StorageInfoVO.builder()
                    .type(storage.getType().getValue())
                    .label(storage.getType().getLabel())
                    .configured(storage.isConfigured())
                    .current(storage.getType() == currentType);
            if (storage.getType() == StorageTypeEnum.COS) {
                builder.bucket(cosClientConfig.getBucket()).endpoint(cosClientConfig.getHost());
            } else {
                builder.bucket(storageProperties.getMinio().getBucket())
                        .endpoint(storageProperties.getMinio().getEndpoint());
            }
            storageInfoList.add(builder.build());
        }
        return StorageConfigVO.builder()
                .currentType(currentType.getValue())
                .currentLabel(currentType.getLabel())
                .storages(storageInfoList)
                .build();
    }
}
