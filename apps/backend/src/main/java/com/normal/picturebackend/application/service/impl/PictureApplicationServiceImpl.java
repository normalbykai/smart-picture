package com.normal.picturebackend.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.normal.picturebackend.application.service.PictureApplicationService;
import com.normal.picturebackend.application.service.SpaceApplicationService;
import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.picture.service.PictureDomainService;
import com.normal.picturebackend.domain.picture.valueobject.PictureCacheConstant;
import com.normal.picturebackend.domain.picture.valueobject.PictureReviewStatusEnum;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.service.SpaceQuotaService;
import com.normal.picturebackend.domain.space.valueobject.SpaceRoleEnum;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.user.service.UserDomainService;
import com.normal.picturebackend.infrastructure.common.DeleteRequest;
import com.normal.picturebackend.infrastructure.common.RedisUtils;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.infrastructure.exception.ThrowUtils;
import com.normal.picturebackend.infrastructure.manager.cache.LocalCacheManage;
import com.normal.picturebackend.infrastructure.manager.file.PictureFileManager;
import com.normal.picturebackend.infrastructure.manager.file.PictureUploadTemplate;
import com.normal.picturebackend.infrastructure.manager.file.PictureUrlManager;
import com.normal.picturebackend.infrastructure.manager.file.model.UploadPictureResult;
import com.normal.picturebackend.infrastructure.manager.file.valueobject.FileConstant;
import com.normal.picturebackend.infrastructure.mapper.PictureMapper;
import com.normal.picturebackend.interfaces.dto.picture.EditPictureRequest;
import com.normal.picturebackend.interfaces.dto.picture.PictureEditBatchRequest;
import com.normal.picturebackend.interfaces.dto.picture.PictureQueryRequest;
import com.normal.picturebackend.interfaces.dto.picture.PictureReviewRequest;
import com.normal.picturebackend.interfaces.dto.picture.PictureUploadByBatchRequest;
import com.normal.picturebackend.interfaces.dto.picture.PictureUploadRequest;
import com.normal.picturebackend.interfaces.vo.picture.PictureVO;
import com.normal.picturebackend.interfaces.vo.user.UserVO;
import com.normal.picturebackend.shared.auth.SpaceUserAuthManager;
import com.normal.picturebackend.shared.auth.StpKit;
import com.normal.picturebackend.shared.auth.valueobject.SpaceUserPermissionConstant;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图片应用服务实现。
 *
 * <p>职责边界：</p>
 * <ul>
 *     <li>编排上传、删除、批量操作等用例，负责事务与权限；</li>
 *     <li>具体的图片校验、审核规则、查询条件构建委托给领域服务；</li>
 *     <li>空间额度变更统一走 {@link SpaceQuotaService}，对象存储清理统一在事务提交后执行。</li>
 * </ul>
 *
 * @author norma
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-04-19 16:54:15
 */
@Service
@Slf4j
public class PictureApplicationServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureApplicationService {

    /**
     * 图片列表缓存 key 前缀，写操作后用于主动失效
     */
    private static final String PICTURE_LIST_CACHE_PREFIX = PictureCacheConstant.PICTURE_LIST_PAGE_VO_KEY_PREFIX;

    /**
     * 单页最大条数，限制爬虫一次拉取过多数据
     */
    private static final long MAX_PAGE_SIZE = 30L;

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private PictureDomainService pictureDomainService;

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private SpaceQuotaService spaceQuotaService;

    @Resource
    private PictureFileManager pictureFileManager;

    @Resource
    private PictureUrlManager pictureUrlManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private LocalCacheManage localCacheManage;

    /**
     * 将图片查询请求转换为查询包装器
     *
     * @param pictureQueryRequest 图片查询请求
     * @return 查询包装类
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        return pictureDomainService.getQueryWrapper(pictureQueryRequest);
    }

    /**
     * 将图片信息转换为 VO
     *
     * @param picture 图片信息
     * @return VO 对象
     */
    @Override
    public PictureVO getPictureVO(Picture picture) {
        if (picture == null) {
            return null;
        }
        Long userId = picture.getUserId();
        PictureVO pictureVO = PictureVO.objToVo(picture);
        if (userId != null && userId > 0) {
            User author = userDomainService.getById(userId);
            UserVO authorVO = userDomainService.getUserVO(author);
            pictureVO.setUserVO(authorVO);
        }
        return pictureVO;
    }

    /**
     * 将分页对象转换为 VO 分页对象
     *
     * @param picturePage 分页的图片对象
     * @param request     用户请求参数
     * @return 分页 VO 对象
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        if (picturePage == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 一次性查出所有作者，避免 N+1 查询
        Set<Long> userIds = pictureList.stream()
                .map(Picture::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userDomainService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(picture -> {
                    PictureVO pictureVO = PictureVO.objToVo(picture);
                    User user = userMap.get(picture.getUserId());
                    if (user != null) {
                        pictureVO.setUserVO(userDomainService.getUserVO(user));
                    }
                    return pictureVO;
                })
                .toList();
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 上传图片（新增或替换）。
     *
     * <p>关键修复点：</p>
     * <ol>
     *     <li>更新图片时未传 spaceId 会复用原图片的 spaceId，避免图片「脱离」空间；</li>
     *     <li>空间额度改为原子增减：新增时 size 与数量同时 +1，替换时只调整大小差额；</li>
     *     <li>替换成功后，在事务提交后删除旧的对象存储文件，避免存储泄漏。</li>
     * </ol>
     *
     * @param inputSource          输入源（MultipartFile 或 URL）
     * @param pictureUploadRequest 图片上传参数
     * @param loginUser            登录用户
     * @return 图片 VO 信息
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 1. 必须登录
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR, "上传参数不能为空");

        // 2. 解析图片 id 与空间 id
        Long pictureId = pictureUploadRequest.getId();
        Long spaceId = pictureUploadRequest.getSpaceId();
        Picture oldPicture = null;

        // 3. 更新场景：校验图片存在、权限、空间一致性
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "更新图片失败，图片不存在");
            // 仅本人或系统管理员可以替换图片文件
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
            }
            if (spaceId == null) {
                // 未传 spaceId 时复用原空间，避免图片被移动到公共图库
                spaceId = oldPicture.getSpaceId();
            } else if (!Objects.equals(spaceId, oldPicture.getSpaceId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间不一致");
            }
        }

        // 4. 校验空间存在、上传权限与配额（先做一次快速失败，最终以原子更新为准）
        Space space = null;
        if (spaceId != null) {
            space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 服务层兜底鉴权：multipart 表单在过滤器阶段可能拿不到 spaceId，
            // 不能只依赖 @SaCheckPermission 的上下文解析，否则可越权上传到任意空间
            checkSpaceUploadPermission(space, loginUser);
            if (pictureId == null) {
                checkSpaceQuotaBeforeUpload(space);
            }
        }

        // 5. 生成对象键并上传到当前对象存储
        String uploadPathPrefix = spaceId == null
                ? String.format("%s/%s/", FileConstant.PUBLIC_DIR, loginUser.getId())
                : String.format("%s/%s/", FileConstant.SPACE_DIR, spaceId);
        PictureUploadTemplate uploadTemplate = inputSource instanceof String ? pictureUrlManager : pictureFileManager;
        UploadPictureResult uploadResult = uploadTemplate.uploadPictureFile(inputSource, uploadPathPrefix);

        // 6. 构建待持久化的图片实体（图片大小统一做空值保护，避免拆箱 NPE）
        long newPicSize = safeSize(uploadResult.getPicSize());
        Picture picture = Picture.builder()
                .url(uploadResult.getUrl())
                .thumbnailUrl(uploadResult.getThumbnailUrl())
                .name(uploadResult.getPicName())
                .picSize(newPicSize)
                .picWidth(uploadResult.getPicWidth())
                .picHeight(uploadResult.getPicHeight())
                .picScale(uploadResult.getPicScale())
                .picFormat(uploadResult.getPicFormat())
                .userId(loginUser.getId())
                .spaceId(spaceId)
                .mainColor(uploadResult.getMainColor())
                .originUrl(uploadResult.getOriginUrl())
                .build();
        if (StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picture.setName(pictureUploadRequest.getPicName());
        }
        this.fillReviewParams(picture, loginUser);

        // 7. 事务内持久化并调整额度
        Long finalSpaceId = spaceId;
        Picture oldPictureForCleanup = oldPicture;
        PictureVO pictureVO;
        try {
            pictureVO = transactionTemplate.execute(status -> {
                if (pictureId != null) {
                    // 替换：数量不变，只调整大小差额
                    picture.setId(pictureId);
                    picture.setEditTime(new Date());
                    if (finalSpaceId != null) {
                        long delta = newPicSize - safeSize(oldPictureForCleanup.getPicSize());
                        spaceQuotaService.adjustSize(finalSpaceId, delta);
                    }
                    boolean updated = this.updateById(picture);
                    if (!updated) {
                        // 内容与数据库完全一致时 affected rows 可能为 0，这里不作为失败处理
                        log.info("图片内容未发生变化，pictureId={}", pictureId);
                    }
                } else {
                    // 新增：大小与数量同时增加
                    if (finalSpaceId != null) {
                        spaceQuotaService.increase(finalSpaceId, newPicSize);
                    }
                    boolean saved = this.save(picture);
                    ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "图片上传失败，数据库错误");
                }
                return getPictureVO(picture);
            });
        } catch (RuntimeException e) {
            // 事务失败（额度不足、数据库异常等）：清理刚上传的新文件，避免产生孤儿对象
            deleteStorageFilesQuietly(
                    uploadResult.getOriginUrl(),
                    uploadResult.getUrl(),
                    uploadResult.getThumbnailUrl()
            );
            throw e;
        }

        // 8. 事务提交后删除被替换的旧文件，避免对象存储泄漏
        if (oldPictureForCleanup != null) {
            deleteStorageFilesQuietly(
                    oldPictureForCleanup.getOriginUrl(),
                    oldPictureForCleanup.getUrl(),
                    oldPictureForCleanup.getThumbnailUrl()
            );
        }
        // 9. 主动失效图片列表缓存
        evictPictureListCache();
        return pictureVO;
    }

    /**
     * 校验用户是否拥有空间上传权限。
     *
     * <p>私人空间仅创建者/系统管理员，团队空间需要成员角色包含 {@code picture:upload}。</p>
     *
     * @param space     空间
     * @param loginUser 登录用户
     */
    private void checkSpaceUploadPermission(Space space, User loginUser) {
        if (loginUser.isAdmin()) {
            return;
        }
        List<String> permissions = spaceUserAuthManager.getPermissionsList(space, loginUser);
        ThrowUtils.throwIf(!permissions.contains(SpaceUserPermissionConstant.PICTURE_UPLOAD),
                ErrorCode.NOT_AUTH_ERROR, "没有该空间的上传权限");
    }

    /**
     * 上传前的空间配额快速校验
     *
     * @param space 空间
     */
    private void checkSpaceQuotaBeforeUpload(Space space) {
        long maxCount = space.getMaxCount() == null ? Long.MAX_VALUE : space.getMaxCount();
        long maxSize = space.getMaxSize() == null ? Long.MAX_VALUE : space.getMaxSize();
        long totalCount = space.getTotalCount() == null ? 0L : space.getTotalCount();
        long totalSize = space.getTotalSize() == null ? 0L : space.getTotalSize();
        ThrowUtils.throwIf(totalCount >= maxCount, ErrorCode.PARAMS_ERROR, "空间条数已满");
        ThrowUtils.throwIf(totalSize >= maxSize, ErrorCode.PARAMS_ERROR, "空间大小不足");
    }

    /**
     * 管理员图片审核
     *
     * @param pictureReviewRequest 审核参数
     * @param loginUser            登录用户
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        pictureDomainService.doPictureReview(pictureReviewRequest, loginUser);
        evictPictureListCache();
    }

    /**
     * 填充审核信息
     *
     * @param picture   图片
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        pictureDomainService.fillReviewParams(picture, loginUser);
    }

    /**
     * 通过关键词批量抓取网络图片并上传（仅管理员）。
     *
     * <p>该逻辑属于「用例编排」，放在应用层；领域层只保留图片规则，不再依赖 Jsoup 与网络。</p>
     *
     * @param request   批量上传请求
     * @param loginUser 登录用户
     * @return 上传成功的数量
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "图片上传批量参数错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);
        String searchText = request.getSearchText();
        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");

        Integer number = request.getNumber();
        int targetCount = number == null ? 10 : number;
        ThrowUtils.throwIf(targetCount <= 0 || targetCount > 20, ErrorCode.PARAMS_ERROR, "单次批量上传数量需在 1-20 之间");

        String namePrefix = StrUtil.blankToDefault(request.getNamePrefix(), searchText);
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).timeout(5000).get();
        } catch (IOException e) {
            log.error("批量上传获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }

        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String imgUrl = imgElement.attr("src");
            if (StringUtils.isBlank(imgUrl)) {
                log.info("当前链接为空，已跳过: {}", imgUrl);
                continue;
            }
            PictureUploadRequest uploadRequest = new PictureUploadRequest();
            uploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                this.uploadPicture(imgUrl, uploadRequest, loginUser);
                uploadCount++;
            } catch (Exception e) {
                // 单张失败不影响整体，记录日志后继续
                log.error("批量上传单张图片失败，url={}", imgUrl, e);
            }
            if (uploadCount >= targetCount) {
                break;
            }
        }
        return uploadCount;
    }

    /**
     * 编辑图片
     *
     * @param editPictureRequest 编辑参数
     * @param loginUser          登录用户
     */
    @Override
    public void editPicture(EditPictureRequest editPictureRequest, User loginUser) {
        pictureDomainService.editPicture(editPictureRequest, loginUser);
        evictPictureListCache();
    }

    /**
     * 按主色调搜索相似图片
     *
     * @param spaceId   空间 id
     * @param color     目标颜色
     * @param loginUser 登录用户
     * @return 相似图片列表
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, Color color, User loginUser) {
        return pictureDomainService.searchPictureByColor(spaceId, color, loginUser);
    }

    /**
     * 批量编辑空间内属于当前用户的图片
     *
     * @param request   批量编辑请求
     * @param loginUser 登录用户
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editPictureBatch(PictureEditBatchRequest request, User loginUser) {
        ThrowUtils.throwIf(ObjUtil.isNull(request), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);
        Long spaceId = request.getSpaceId();
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR, "空间 id 不能为空");
        List<Long> pictureIdList = request.getPictureIdList();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR, "请选择要编辑的图片");
        // 只允许修改自己上传的图片；空间访问权限已由 @SaCheckPermission 保证
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .eq(Picture::getUserId, loginUser.getId())
                .in(Picture::getId, pictureIdList)
                .list();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureList), ErrorCode.NOT_FOUND_ERROR, "没有可编辑的图片");

        // 1. 更新分类与标签
        String category = request.getCategory();
        List<String> tags = request.getTags();
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        // 2. 按命名规则批量重命名
        fillPictureWithNameRule(pictureList, request.getNameRule());

        // 3. 批量更新
        boolean updated = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "批量修改图片数据失败");
        evictPictureListCache();
        return true;
    }

    /**
     * 删除图片。
     *
     * <p>数据库删除与额度释放放在同一事务；对象存储删除放在事务提交之后，
     * 避免「文件已删、事务回滚」导致的数据不一致。</p>
     *
     * @param deleteRequest 删除请求
     * @param loginUser     登录用户
     * @return true 表示删除成功
     */
    @Override
    public Boolean deletePicture(DeleteRequest deleteRequest, User loginUser) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);

        // 1. 校验图片存在
        Long pictureId = deleteRequest.getId();
        Picture oldPicture = pictureDomainService.getById(pictureId);
        ThrowUtils.throwIf(ObjUtil.isNull(oldPicture), ErrorCode.NOT_FOUND_ERROR, "删除的图片不存在");

        // 2. 事务内删除数据库记录并释放额度
        Boolean result = transactionTemplate.execute(status -> {
            boolean removed = pictureDomainService.removeById(pictureId);
            ThrowUtils.throwIf(!removed, ErrorCode.SYSTEM_ERROR, "图片删除失败，数据库错误");
            if (oldPicture.getSpaceId() != null) {
                spaceQuotaService.decrease(oldPicture.getSpaceId(), safeSize(oldPicture.getPicSize()));
            }
            return true;
        });

        // 3. 事务提交后删除对象存储文件
        deleteStorageFilesQuietly(oldPicture.getOriginUrl(), oldPicture.getUrl(), oldPicture.getThumbnailUrl());
        evictPictureListCache();
        return result;
    }

    /**
     * 更新图片信息
     *
     * @param picture   图片
     * @param loginUser 登录用户
     * @return true 表示更新成功
     */
    @Override
    public boolean updatePicture(Picture picture, User loginUser) {
        boolean updated = pictureDomainService.updatePicture(picture, loginUser);
        evictPictureListCache();
        return updated;
    }

    /**
     * 通过 id 获取图片 VO，并填充当前用户在该空间的权限
     *
     * @param id        图片 id
     * @param loginUser 登录用户
     * @return 图片 VO
     */
    @Override
    public PictureVO getPictureVOById(long id, User loginUser) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureDomainService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isNull(picture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 1. 空间图片需要校验查看权限
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            boolean hasViewPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasViewPermission, ErrorCode.NOT_AUTH_ERROR, "没有查看权限");
            space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isNull(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }

        // 2. 构建 VO 并填充权限列表
        PictureVO pictureVO = pictureDomainService.getPictureVO(picture);
        if (loginUser != null) {
            List<String> permissions = spaceUserAuthManager.getPermissionsList(space, loginUser);
            if (picture.getUserId().equals(loginUser.getId())) {
                // 图片创建者拥有全部权限
                permissions = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
            }
            pictureVO.setPermissions(permissions);
        } else {
            pictureVO.setPermissions(Collections.emptyList());
        }
        return pictureVO;
    }

    /**
     * 分页查询图片（返回实体，供管理端使用）
     *
     * @param pictureQueryRequest 查询参数
     * @param loginUser           登录用户
     * @return 图片分页
     */
    @Override
    public Page<Picture> getPictureVOByPage(PictureQueryRequest pictureQueryRequest, User loginUser) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long currentNum = pictureQueryRequest.getCurrentNum() <= 0 ? 1 : pictureQueryRequest.getCurrentNum();
        long pageSize = pictureQueryRequest.getPageSize() <= 0 ? 10 : pictureQueryRequest.getPageSize();
        // 限制单页数量，降低爬虫与慢查询风险
        ThrowUtils.throwIf(pageSize > MAX_PAGE_SIZE, ErrorCode.PARAMS_ERROR, "每页最多查询 30 条");

        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            // 公共图库：只展示审核通过的图片，且 spaceId 必须为空
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            // 空间图库：校验查看权限
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NOT_AUTH_ERROR, "没有空间权限");
            // 明确查询当前空间，避免领域层默认按「公共图库」处理
            pictureQueryRequest.setNullSpaceId(false);
        }
        QueryWrapper<Picture> queryWrapper = pictureDomainService.getQueryWrapper(pictureQueryRequest);
        return pictureDomainService.page(new Page<>(currentNum, pageSize), queryWrapper);
    }

    /**
     * 多级缓存分页查询图片 VO（本地缓存 -> Redis -> 数据库）
     *
     * @param pictureQueryRequest 查询参数
     * @param userLogin           登录用户
     * @return 图片 VO 分页
     */
    @Override
    public Page<PictureVO> getPictureVOByPageUseCache(PictureQueryRequest pictureQueryRequest, User userLogin) {
        return pictureDomainService.getPictureVOPageUseCache(pictureQueryRequest, userLogin, () -> {
            Page<Picture> picturePage = this.getPictureVOByPage(pictureQueryRequest, userLogin);
            return this.getPictureVOPage(picturePage, null);
        });
    }

    /**
     * 按命名规则批量重命名图片
     *
     * <p>规则中的 {@code {序号}} 会被替换为从 1 开始的自增序号。</p>
     *
     * @param pictureList 图片列表
     * @param nameRule    命名规则
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                picture.setName(nameRule.replace("{序号}", String.valueOf(count++)));
            }
        } catch (Exception e) {
            log.error("批量修改图片名称失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "名称解析失败");
        }
    }

    /**
     * 图片大小空值保护
     *
     * @param size 图片大小
     * @return 非空大小
     */
    private static long safeSize(Long size) {
        return size == null ? 0L : size;
    }

    /**
     * 事务提交后删除对象存储文件。
     *
     * <p>这里只做「尽力而为」的清理：删除失败只记录日志，不影响主流程；
     * 同一张图片的 url / originUrl / thumbnailUrl 可能相同，先去重。</p>
     *
     * @param urls 待删除的地址
     */
    private void deleteStorageFilesQuietly(String... urls) {
        if (urls == null || urls.length == 0) {
            return;
        }
        Set<String> distinctUrls = Arrays.stream(urls)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (String url : distinctUrls) {
            try {
                pictureFileManager.deletePicture(url);
            } catch (Exception e) {
                log.error("删除对象存储文件失败，url={}", url, e);
            }
        }
    }

    /**
     * 主动失效图片列表缓存（本地 + Redis）
     *
     * <p>图片新增/删除/编辑后调用，避免用户看到过期列表。</p>
     */
    private void evictPictureListCache() {
        try {
            localCacheManage.clearCache();
            redisUtils.deleteByKeyPrefix(PICTURE_LIST_CACHE_PREFIX);
        } catch (Exception e) {
            // 缓存清理失败不影响主流程
            log.warn("清理图片列表缓存失败: {}", e.getMessage());
        }
    }
}
