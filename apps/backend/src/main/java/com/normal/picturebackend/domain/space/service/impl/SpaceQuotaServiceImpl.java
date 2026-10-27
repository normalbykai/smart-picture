package com.normal.picturebackend.domain.space.service.impl;

import com.normal.picturebackend.domain.space.repository.SpaceRepository;
import com.normal.picturebackend.domain.space.service.SpaceQuotaService;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 空间额度领域服务实现。
 *
 * <p>实现上只做参数校验与错误语义转换，真正的原子更新交给
 * {@link SpaceRepository#applyQuotaChange(Long, long, long)}。</p>
 *
 * @author norma
 */
@Service
@Slf4j
public class SpaceQuotaServiceImpl implements SpaceQuotaService {

    @Resource
    private SpaceRepository spaceRepository;

    /**
     * 增加额度（新增一张图片）
     *
     * @param spaceId 空间 id
     * @param size    图片大小（字节）
     */
    @Override
    public void increase(Long spaceId, long size) {
        if (spaceId == null) {
            return;
        }
        if (size < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小不能为负数");
        }
        boolean success = spaceRepository.applyQuotaChange(spaceId, size, 1);
        if (!success) {
            // 条件更新失败：空间不存在，或容量/数量已达上限
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容量或图片数量已满");
        }
    }

    /**
     * 释放额度（删除一张图片）
     *
     * @param spaceId 空间 id
     * @param size    图片大小（字节）
     */
    @Override
    public void decrease(Long spaceId, long size) {
        if (spaceId == null) {
            return;
        }
        if (size < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小不能为负数");
        }
        boolean success = spaceRepository.applyQuotaChange(spaceId, -size, -1);
        if (!success) {
            // 空间可能已被并发删除；这里只记录日志，不阻断图片删除
            log.warn("释放空间额度失败，空间可能已被删除: spaceId={}, size={}", spaceId, size);
        }
    }

    /**
     * 仅调整大小额度（图片替换时使用）
     *
     * @param spaceId   空间 id
     * @param sizeDelta 大小增量
     */
    @Override
    public void adjustSize(Long spaceId, long sizeDelta) {
        if (spaceId == null || sizeDelta == 0) {
            return;
        }
        boolean success = spaceRepository.applyQuotaChange(spaceId, sizeDelta, 0);
        if (!success && sizeDelta > 0) {
            // 只有增加占用时才视为失败；减少占用失败通常说明空间已被删除
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容量已满，无法替换为更大的图片");
        }
        if (!success) {
            log.warn("释放空间额度失败，空间可能已被删除: spaceId={}, sizeDelta={}", spaceId, sizeDelta);
        }
    }
}
