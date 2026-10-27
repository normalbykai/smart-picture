package com.normal.picturebackend.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.repository.SpaceRepository;
import com.normal.picturebackend.infrastructure.mapper.SpaceMapper;
import org.springframework.stereotype.Repository;

/**
 * 空间仓储实现。
 *
 * <p>额度变更使用单条 UPDATE 完成，依赖数据库行锁保证并发安全，
 * 不再使用「先查再算再写」的非原子方式。</p>
 */
@Repository
public class SpaceRepositoryImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceRepository {

    /**
     * 原子调整空间额度
     *
     * @param spaceId    空间 id
     * @param sizeDelta  大小增量
     * @param countDelta 数量增量
     * @return true 表示更新成功
     */
    @Override
    public boolean applyQuotaChange(Long spaceId, long sizeDelta, long countDelta) {
        if (spaceId == null) {
            return false;
        }
        // 增量为 long 基本类型，不存在 SQL 注入风险；GREATEST + COALESCE 保证扣减后不会出现负数或 NULL
        String sizeSql = String.format(
                "totalSize = GREATEST(COALESCE(totalSize, 0) + (%d), 0)", sizeDelta);
        String countSql = String.format(
                "totalCount = GREATEST(COALESCE(totalCount, 0) + (%d), 0)", countDelta);

        UpdateWrapper<Space> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", spaceId)
                .setSql(sizeSql)
                .setSql(countSql);
        // 只有增加额度时才校验上限；减少额度始终允许
        if (sizeDelta > 0) {
            updateWrapper.apply("COALESCE(totalSize, 0) + {0} <= maxSize", sizeDelta);
        }
        if (countDelta > 0) {
            updateWrapper.apply("COALESCE(totalCount, 0) + {0} <= maxCount", countDelta);
        }
        return baseMapper.update(null, updateWrapper) > 0;
    }
}
