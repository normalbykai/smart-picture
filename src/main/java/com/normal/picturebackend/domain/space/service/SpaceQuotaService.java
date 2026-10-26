package com.normal.picturebackend.domain.space.service;

/**
 * 空间额度领域服务。
 *
 * <p>把「空间容量/数量」的校验与变更收敛到一处，供图片上传、删除、替换等场景复用。
 * 所有变更都通过仓储层的原子 UPDATE 完成，避免并发下超额。</p>
 *
 * @author norma
 */
public interface SpaceQuotaService {

    /**
     * 增加额度（新增一张图片）
     *
     * @param spaceId 空间 id
     * @param size    图片大小（字节）
     */
    void increase(Long spaceId, long size);

    /**
     * 释放额度（删除一张图片）
     *
     * @param spaceId 空间 id
     * @param size    图片大小（字节）
     */
    void decrease(Long spaceId, long size);

    /**
     * 仅调整大小额度（图片替换时使用）
     *
     * @param spaceId    空间 id
     * @param sizeDelta  大小增量，正数表示新增占用，负数表示释放
     */
    void adjustSize(Long spaceId, long sizeDelta);
}
