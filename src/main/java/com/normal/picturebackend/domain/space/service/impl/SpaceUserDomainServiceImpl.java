package com.normal.picturebackend.domain.space.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.normal.picturebackend.domain.space.entity.SpaceUser;
import com.normal.picturebackend.domain.space.repository.SpaceUserRepository;
import com.normal.picturebackend.domain.space.service.SpaceUserDomainService;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.normal.picturebackend.infrastucture.exception.ThrowUtils;
import com.normal.picturebackend.infrastucture.mapper.SpaceUserMapper;
import com.normal.picturebackend.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 空间成员领域服务实现。
 *
 * <p>只负责成员关联的查询条件构建与基础 CRUD，不依赖 application 层。</p>
 *
 * @author norma
 * @description 针对表【space_user(空间用户关联表)】的数据库操作Service实现
 * @createDate 2025-06-08 00:13:18
 */
@Service
public class SpaceUserDomainServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserDomainService {

    @Resource
    private SpaceUserRepository spaceUserRepository;

    /**
     * 构建空间成员查询条件
     *
     * @param spaceUserQueryRequest 查询参数
     * @return 查询包装器
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);

        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();

        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }
}
