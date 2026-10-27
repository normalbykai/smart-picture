package com.normal.picturebackend.domain.space.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.repository.SpaceRepository;
import com.normal.picturebackend.domain.space.service.SpaceDomainService;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.infrastructure.exception.ThrowUtils;
import com.normal.picturebackend.infrastructure.mapper.SpaceMapper;
import com.normal.picturebackend.infrastructure.utils.SortFieldUtils;
import com.normal.picturebackend.interfaces.dto.space.SpaceQueryRequest;
import com.normal.picturebackend.interfaces.vo.space.SpaceUsageAnalyzeResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
* @author norma
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-05-04 14:17:22
*/

@Slf4j
@Service
public class SpaceDomainServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceDomainService {

    /**
     * 空间表允许排序的字段白名单（防止 ORDER BY 注入）
     */
    private static final Set<String> SPACE_SORT_FIELDS = Set.of(
            "id", "spaceName", "spaceLevel", "spaceType",
            "totalSize", "totalCount", "createTime", "updateTime"
    );

    @Resource
    private SpaceRepository spaceRepository;

    /**
     * 空间参数请求构建为查询包装器
     *
     * @param spaceQueryRequest 查询条件参数
     * @return 查询包装器
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);

        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();

        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id),"id", id);
        queryWrapper.eq(ObjUtil.isNotNull(spaceType), "spaceType", spaceType);
        queryWrapper.eq(ObjUtil.isNotNull(userId),"userId", userId);
        queryWrapper.eq(ObjUtil.isNotNull(spaceLevel),"spaceLevel", spaceLevel);
        queryWrapper.like(StrUtil.isNotBlank(spaceName),"spaceName", spaceName);

        // 排序字段走白名单，避免 ORDER BY 注入
        String safeSortField = SortFieldUtils.resolve(sortField, SPACE_SORT_FIELDS, "createTime");
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(true, SortFieldUtils.isAsc(sortOrder), safeSortField);
        }

        return queryWrapper;
    }


    /**
     * 校验私有空间权限
     *
     * @param loginUser
     * @param spaceId
     * @return
     */
    @Override
    public Space checkSpaceAuth(User loginUser, Long spaceId) {
        // 1. 校验用户是否登录
        if (loginUser == null ){
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }

        // 2. 校验空间是否存在
        Space oldSpace = this.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 3. 空间权限校验 - (仅本人)或者(系统管理员)
        // 空间创建人和登录用户不是同一个人 && 不是管理员
        if (oldSpace.isNotSpaceCreator(loginUser.getId()) && loginUser.isNotAdmin()) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }

        // 4. 返回校验后的空间
        return oldSpace;
    }

    /**
     * 空间使用情况分析
     *
     * @param spaceId 空间id
     * @return 使用情况统计
     */
    @Override
    public SpaceUsageAnalyzeResponse countSpaceUsageAnalyze(Long spaceId) {
        // 1. 校验空间存在
        Space oldSpace = spaceRepository.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 2. 读取累计值并做空值保护
        long usedSize = oldSpace.getTotalSize() == null ? 0L : oldSpace.getTotalSize();
        long usedCount = oldSpace.getTotalCount() == null ? 0L : oldSpace.getTotalCount();
        long maxSize = oldSpace.getMaxSize() == null ? 0L : oldSpace.getMaxSize();
        long maxCount = oldSpace.getMaxCount() == null ? 0L : oldSpace.getMaxCount();

        // 3. 构建封装类：注意两个比例字段不能写反，且上限为 0 时直接返回 0 避免除零
        SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
        response.setMaxCount(maxCount);
        response.setMaxSize(maxSize);
        response.setUsedSize(usedSize);
        response.setUsedCount(usedCount);
        response.setSizeUsageRatio(maxSize <= 0 ? 0.0 : NumberUtil.round(usedSize * 100.0 / maxSize, 2).doubleValue());
        response.setCountUsageRatio(maxCount <= 0 ? 0.0 : NumberUtil.round(usedCount * 100.0 / maxCount, 2).doubleValue());
        return response;
    }

}




