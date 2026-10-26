package com.normal.picturebackend.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.picture.repository.PictureRepository;
import com.normal.picturebackend.domain.space.service.SpaceDomainService;
import com.normal.picturebackend.domain.space.service.SpaceUserDomainService;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.normal.picturebackend.infrastucture.exception.ThrowUtils;
import com.normal.picturebackend.infrastucture.manager.file.PictureFileManager;
import com.normal.picturebackend.infrastucture.mapper.SpaceMapper;
import com.normal.picturebackend.interfaces.assembler.SpaceAssembler;
import com.normal.picturebackend.interfaces.dto.space.SpaceAddRequest;
import com.normal.picturebackend.interfaces.dto.space.SpaceQueryRequest;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.entity.SpaceUser;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.space.valueobject.SpaceLevelEnum;
import com.normal.picturebackend.domain.space.valueobject.SpaceRoleEnum;
import com.normal.picturebackend.domain.space.valueobject.SpaceTypeEnum;
import com.normal.picturebackend.interfaces.vo.space.SpaceVO;
import com.normal.picturebackend.application.service.SpaceApplicationService;
import com.normal.picturebackend.domain.user.service.UserDomainService;
import com.normal.picturebackend.shared.auth.SpaceUserAuthManager;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author norma
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-05-04 14:17:22
 */
@Service
@Slf4j
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceApplicationService {

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private SpaceDomainService spaceDomainService;

    @Resource
    private SpaceUserDomainService spaceUserDomainService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private PictureRepository pictureRepository;

    @Resource
    private PictureFileManager pictureFileManager;

    /**
     * 单机内的空间创建锁：按用户维度串行化，避免同一用户并发创建出两个同类空间。
     * 多实例部署时需要数据库唯一索引或分布式锁兜底。
     */
    private static final ConcurrentHashMap<Long, Object> SPACE_CREATE_LOCKS = new ConcurrentHashMap<>();

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 1. null对象直接返回,无需转换
        if (space == null) {
            return null;
        }
        // 2. 转化为基础字段: Space中拥有的字段到SpaceVO中
        SpaceVO spaceVO = SpaceAssembler.toSpaceVO(space);

        // 3. 填充创建人信息(仅在存在userId时查询)
        if (ObjUtil.isNotEmpty(spaceVO.getUserId())) {
            // 注意: 获取用户信息时, 需要使用事务, 否则会报错
            User user = userDomainService.getUserById(spaceVO.getUserId());
            // 填充用户脱敏信息
            spaceVO.setUser(user.convertToVO());
        }

        // 4. 获取当前登录用户在该空间中拥有的权限，提供给前端用于组件的权限设计管理
        User loginUser = userDomainService.getUserLogin(request);
        spaceVO.setPermissions(spaceUserAuthManager.getPermissionsList(space, loginUser));

        // 5. 返回封装类
        return spaceVO;
    }

    /**
     * 将分页对象 转换为VO
     *
     * @param spacePage 分页的图片对象
     * @param request   用户请求参数
     * @return 分页VO对象
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        if (spacePage == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 1. 构建分页空间封装类对象
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize());

        // 2. 待转换的分页空间数据列表
        List<Space> spaceList = spacePage.getRecords();
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage; // 空数据直接返回
        }

        // 3. 批量获取关联的用户数据
        Set<Long> userIds = spaceList.stream()
                .map(Space::getUserId).collect(Collectors.toSet());
        // 4.一次性查询空间分页关联的相关用户, 并构建为Map映射(userId:Long, user:User), 为后续封装用户信息
        List<User> userList = userDomainService.listByIds(userIds);
        Map<Long, User> userIdUserVOList = userList.stream().collect(Collectors.toMap(User::getId, Function.identity()));

        // 5. 开始转换为空间封装类集合
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(space -> {
                    Long userId = space.getUserId();
                    User user = null;
                    SpaceVO spaceVO = SpaceAssembler.toSpaceVO( space);;
                    if (userIdUserVOList.containsKey(userId)) {
                        user = userIdUserVOList.get(userId);
                    }
                    spaceVO.setUser(userDomainService.getUserVO(user));
                    return spaceVO;
                }).toList();

        // 6. 设置分页封装类的记录（空间封装类数据）
        spaceVOPage.setRecords(spaceVOList);

        // 7.将VO包装类返回
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    @Override
    // TODO 增强功能: 管理员能通过userId参数为该用户创建空间
    @Transactional(rollbackFor = Exception.class)
    public Long addSpace(SpaceAddRequest spaceAddRequest, User userLogin) {
        // 1. 参数校验
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 设置空间默认参数, 无参数时使用默认参数
        String spaceName = spaceAddRequest.getSpaceName();
        Integer spaceLevel = spaceAddRequest.getSpaceLevel();
        Integer spaceType = spaceAddRequest.getSpaceType();
        if (StrUtil.isBlank(spaceName)) {
            // 空间名称
            spaceAddRequest.setSpaceName("默认空间");
        }
        if (spaceLevel == null) {
            // 空间级别 (普通级别)
            spaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (ObjUtil.isEmpty(spaceType)) {
            // 空间类型 (私人空间)
            spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }

        // 3. 封装空间对象
        // 空间参数转换,校验空间参数
        Space space = SpaceAssembler.toSpaceEntity(spaceAddRequest);
        space.validate(true);
        // 填充空间级别相关信息(空间图片最大数量/空间容最大容量/)
        space.fillSpaceBySpaceLevel();
        space.setUserId(userLogin.getId());
        space.setSpaceType(space.getSpaceType());

        // 4. 创建空间 操作数据库
        // 加锁：按用户维度串行化，避免并发创建出两个同类空间
        Object lock = SPACE_CREATE_LOCKS.computeIfAbsent(userLogin.getId(), key -> new Object());
        return transactionTemplate.execute(status -> {
            // 针对用户进行加锁
            synchronized (lock) {
                // 判断空间是否存在
                boolean exists = spaceDomainService.lambdaQuery()
                        .eq(Space::getUserId, userLogin.getId())
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间只能创建一个");

                // 持久化数据
                boolean save = spaceDomainService.save(space);
                ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "空间创建失败，数据库错误");

                // 创建的为团队空间才进行空间用户关联
                if (space.getSpaceType() == SpaceTypeEnum.TEEM.getValue()) {
                    // 构建空间用户关联
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(space.getUserId());
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue()); // 创建人默认都是当前空间的管理员角色

                    // 持久化空间用数据
                    boolean spaceUserSaveState = spaceUserDomainService.save(spaceUser);
                    ThrowUtils.throwIf(!spaceUserSaveState, ErrorCode.SYSTEM_ERROR, "空间用户关联数据创建失败，数据库错误");
                }
                return space.getId();
            }
        });

    }

    /**
     * 删除空间。
     *
     * <p>删除顺序：图片（逻辑删除）→ 空间成员 → 空间本身；
     * 对象存储文件在事务提交后异步清理，删除失败只记录日志，不影响主流程。</p>
     *
     * @param spaceId   空间 id
     * @param loginUser 登录用户
     * @return true 表示删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSpace(Long spaceId, User loginUser) {
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);

        // 1. 校验空间存在与权限
        Space space = this.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "存储空间不存在");
        if (!space.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }

        // 2. 先查出空间内图片，用于事务提交后清理对象存储
        List<Picture> pictureList = pictureRepository.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .list();

        // 3. 逻辑删除图片（没有图片时 remove 返回 false，属于正常情况，不能当作失败）
        pictureRepository.lambdaUpdate()
                .eq(Picture::getSpaceId, spaceId)
                .remove();

        // 4. 删除空间成员关联
        spaceUserDomainService.lambdaUpdate()
                .eq(SpaceUser::getSpaceId, spaceId)
                .remove();

        // 5. 删除空间本身
        boolean removed = this.removeById(spaceId);
        ThrowUtils.throwIf(!removed, ErrorCode.SYSTEM_ERROR, "空间删除失败，数据库错误");

        // 6. 事务提交后清理对象存储文件
        runAfterCommit(() -> deletePictureFilesQuietly(pictureList));
        return true;
    }

    /**
     * 事务提交后执行清理任务
     *
     * @param task 清理任务
     */
    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            // 没有事务时直接执行
            task.run();
        }
    }

    /**
     * 删除图片对应的对象存储文件（尽力而为）
     *
     * @param pictureList 图片列表
     */
    private void deletePictureFilesQuietly(List<Picture> pictureList) {
        if (CollUtil.isEmpty(pictureList)) {
            return;
        }
        Set<String> urls = new LinkedHashSet<>();
        for (Picture picture : pictureList) {
            addUrlIfNotBlank(urls, picture.getOriginUrl());
            addUrlIfNotBlank(urls, picture.getUrl());
            addUrlIfNotBlank(urls, picture.getThumbnailUrl());
        }
        for (String url : urls) {
            try {
                pictureFileManager.deletePicture(url);
            } catch (Exception e) {
                log.error("删除空间图片文件失败，url={}", url, e);
            }
        }
    }

    /**
     * 收集非空 URL
     *
     * @param urls URL 集合
     * @param url  待收集的 URL
     */
    private void addUrlIfNotBlank(Set<String> urls, String url) {
        if (StrUtil.isNotBlank(url)) {
            urls.add(url);
        }
    }

    @Override
    public Space checkSpaceAuth(User loginUser, Long spaceId) {
        return spaceDomainService.checkSpaceAuth(loginUser, spaceId);
    }

}




