package com.normal.picturebackend.domain.space.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.normal.picturebackend.domain.space.entity.Space;


/**
 * 空间仓储接口。
 *
 * <p>除基础 CRUD 外，把「额度变更」这种需要原子性的操作收敛到仓储层，
 * 由实现类用一条带条件的 UPDATE 完成，避免并发下的超卖。</p>
 */
public interface SpaceRepository extends IService<Space> {

    /**
     * 原子地调整空间额度
     *
     * <p>SQL 语义：</p>
     * <pre>
     * UPDATE space
     * SET totalSize  = GREATEST(totalSize + sizeDelta, 0),
     *     totalCount = GREATEST(totalCount + countDelta, 0)
     * WHERE id = ?
     *   AND (sizeDelta  &lt;= 0 OR totalSize  + sizeDelta  &lt;= maxSize)
     *   AND (countDelta &lt;= 0 OR totalCount + countDelta &lt;= maxCount)
     * </pre>
     *
     * @param spaceId    空间 id
     * @param sizeDelta  大小增量（正数增加、负数减少，单位字节）
     * @param countDelta 数量增量（正数增加、负数减少）
     * @return true 表示更新成功；false 表示空间不存在或额度不足
     */
    boolean applyQuotaChange(Long spaceId, long sizeDelta, long countDelta);
}
