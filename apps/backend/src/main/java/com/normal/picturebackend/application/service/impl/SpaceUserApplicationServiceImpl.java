package com.normal.picturebackend.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.normal.picturebackend.domain.space.service.SpaceDomainService;
import com.normal.picturebackend.domain.space.service.SpaceUserDomainService;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.infrastructure.exception.ThrowUtils;
import com.normal.picturebackend.infrastructure.mapper.SpaceUserMapper;
import com.normal.picturebackend.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.normal.picturebackend.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.entity.SpaceUser;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.space.valueobject.SpaceRoleEnum;
import com.normal.picturebackend.interfaces.vo.spaceuser.SpaceUserVO;
import com.normal.picturebackend.interfaces.assembler.SpaceUserAssembler;
import com.normal.picturebackend.interfaces.vo.space.SpaceVO;
import com.normal.picturebackend.interfaces.vo.user.UserVO;
import com.normal.picturebackend.application.service.SpaceUserApplicationService;
import com.normal.picturebackend.domain.user.service.UserDomainService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author norma
* @description 针对表【space_user(空间用户关联表)】的数据库操作Service实现
* @createDate 2025-06-08 00:13:18
*/
@Service
public class SpaceUserApplicationServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserApplicationService {

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private SpaceDomainService spaceDomainService;

    @Resource
    private SpaceUserDomainService spaceUserDomain;


    @Override
    public SpaceUserVO converseSpaceUserToVO(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }
        Long userId = spaceUser.getUserId();
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        if (userId != null && userId > 0) {
            User user = userDomainService.getById(userId);
            UserVO userVO = userDomainService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceVOList(List<SpaceUser> spaceUserList) {
        // 为空无需转换直接返回空集合
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }

        // 将space中的 userId 使用set 进行去重查出来
        // 1. 收集需要关联查询的用户ID 和空间ID
        Set<Long> userIds = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIds = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());

        // 通过 in 一次性查找 用户信息 - 空间信息 (减轻数据库查询压力)
        List<User> userList = userDomainService.listByIds(userIds);
        List<Space> spaceList = spaceDomainService.listByIds(spaceIds);

        // 构建userIdMap映射 进行缓存存储 (可通过用户id，拿到用户信息)
        Map<Long, User> userIdUserListMap = userList.stream().collect(Collectors.toMap(User::getId, user -> user));
        // 构建userIdMap映射 进行缓存存储 (可通过空间id，拿到空间信息)
        Map<Long, Space> spaceIdSpaceListMap = spaceList.stream().collect(Collectors.toMap(Space::getId, space -> space));

        // 2. 转换为VO对象
        List<SpaceUserVO> spaceVOList = spaceUserList.stream().map(spaceUser -> {
            Long userId = spaceUser.getUserId();
            Long spaceId = spaceUser.getSpaceId();
            User user = null;
            SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
            // 用户信息
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId);
            }
            spaceUserVO.setUser(userDomainService.getUserVO(user));

            // 空间信息
            Space space =null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
            return spaceUserVO;
        }).toList();

        return spaceVOList;
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceQueryRequest) {
        return spaceUserDomain.getQueryWrapper(spaceQueryRequest);
    }

    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        // 1. 参数校验
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);

        SpaceUser spaceUser = SpaceUserAssembler.toSpaceEntity(spaceUserAddRequest);

        // 3. 空间用户参数校验
        validSpaceUser(spaceUser, true);

        // 4. 持久化数据
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回创建空间关联表的id
        return spaceUser.getId();
    }


    /**
     * 空间用户参数 校验
     * @param spaceUser
     * @param add 是否为添加空间关联表
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);

        // 对象中获取数据
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();

        // 如果是创建空间 则需要判断一下空间名称 和 空间级别
        if (add) {
            // 校验参数 - 必须要填写空间参数
            if (ObjUtil.hasEmpty(spaceId, userId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }

            // 校验用户和空间是否存在
            User user = userDomainService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceDomainService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }

        // 空间角色 校验
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }
}




