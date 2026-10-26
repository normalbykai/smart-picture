package com.normal.picturebackend.domain.picture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.picture.repository.PictureRepository;
import com.normal.picturebackend.domain.picture.service.PictureDomainService;
import com.normal.picturebackend.domain.picture.valueobject.PictureCacheConstant;
import com.normal.picturebackend.domain.picture.valueobject.PictureReviewStatusEnum;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.repository.SpaceRepository;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.user.service.UserDomainService;
import com.normal.picturebackend.infrastucture.common.RedisUtils;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import com.normal.picturebackend.infrastucture.exception.ThrowUtils;
import com.normal.picturebackend.infrastucture.manager.cache.LocalCacheManage;
import com.normal.picturebackend.infrastucture.mapper.PictureMapper;
import com.normal.picturebackend.infrastucture.utils.MainColorSimilarUtils;
import com.normal.picturebackend.infrastucture.utils.SortFieldUtils;
import com.normal.picturebackend.interfaces.assembler.PictureAssembler;
import com.normal.picturebackend.interfaces.dto.picture.EditPictureRequest;
import com.normal.picturebackend.interfaces.dto.picture.PictureQueryRequest;
import com.normal.picturebackend.interfaces.dto.picture.PictureReviewRequest;
import com.normal.picturebackend.interfaces.vo.picture.PictureVO;
import com.normal.picturebackend.interfaces.vo.space.SpaceUsageAnalyzeResponse;
import com.normal.picturebackend.interfaces.vo.user.UserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 图片领域服务实现。
 *
 * <p>这里只保留图片自身的业务规则：查询条件构建、审核、编辑、颜色检索、缓存读取等。
 * 上传/批量上传/批量编辑等「用例编排」已上移到应用层，领域层不再依赖 application 包，
 * 也不再直接操作对象存储。</p>
 *
 * @author norma
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-04-19 16:54:15
 */
@Service
@Slf4j
public class PictureDomainServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureDomainService {

    /**
     * 图片表允许排序的字段白名单（防止 ORDER BY 注入）
     */
    private static final Set<String> PICTURE_SORT_FIELDS = Set.of(
            "id", "name", "category", "picSize", "picWidth", "picHeight", "picScale",
            "createTime", "editTime", "updateTime", "reviewStatus"
    );

    /**
     * 颜色检索最多返回的图片数量
     */
    private static final int COLOR_SEARCH_LIMIT = 15;

    @Resource
    private PictureRepository pictureRepository;

    @Resource
    private SpaceRepository spaceRepository;

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private LocalCacheManage localCacheManage;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 将图片查询请求转换为查询包装器
     *
     * @param pictureQueryRequest 图片查询请求
     * @return 查询包装器
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        if (pictureQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Boolean nullSpaceId = pictureQueryRequest.getNullSpaceId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        // 修复：数据库列名是 userId，之前误写成 loginUserId 会导致 SQL 报错
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);

        // 多字段模糊搜索：name like ? or introduction like ?
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText));
        }

        // 标签是 JSON 数组字符串，这里用包含匹配做简单过滤
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", String.format("%s%s%s", "\"", tag, "\""));
            }
        }

        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);

        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);

        // 编辑时间区间：[startEditTime, endEditTime)
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

        // 空间过滤规则：
        // 1. nullSpaceId=true  -> 只查公共图库（spaceId is null）
        // 2. nullSpaceId=false -> 不追加空值条件（通常与 spaceId 一起使用）
        // 3. 两者都没传        -> 默认公共图库
        if (Boolean.TRUE.equals(nullSpaceId)) {
            queryWrapper.isNull("spaceId");
        } else if (nullSpaceId == null && spaceId == null) {
            queryWrapper.isNull("spaceId");
        }

        // 排序字段走白名单，避免 ORDER BY 注入
        if (StrUtil.isNotBlank(sortField)) {
            String safeSortField = SortFieldUtils.resolve(sortField, PICTURE_SORT_FIELDS, "createTime");
            queryWrapper.orderBy(true, SortFieldUtils.isAsc(sortOrder), safeSortField);
        }
        return queryWrapper;
    }

    /**
     * 图片信息校验
     *
     * @param picture 图片信息
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时 id 不能为空
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
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
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User author = userDomainService.getById(userId);
            UserVO authorVO = userDomainService.getUserVO(author);
            pictureVO.setUserVO(authorVO);
        }
        return pictureVO;
    }

    /**
     * 将图片信息转换为 VO（保留 request 参数以兼容旧调用）
     *
     * @param picture 图片信息
     * @param request 请求
     * @return VO 对象
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        return getPictureVO(picture);
    }

    /**
     * 管理员审核图片
     *
     * @param pictureReviewRequest 审核参数
     * @param loginUser            登录用户
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);

        // id 必须存在，审核状态必须合法且不能是「待审核」
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.PENDING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Picture oldPicture = pictureRepository.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 已经是该状态时无需重复审核
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }

        Picture updateReviewPicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updateReviewPicture);
        updateReviewPicture.setReviewerId(loginUser.getId());
        boolean updated = pictureRepository.updateById(updateReviewPicture);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 填充审核信息：管理员自动过审，普通用户进入待审核
     *
     * @param picture   图片
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (picture == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核填充失败");
        }
        if (loginUser.isAdmin()) {
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewTime(new Date());
        } else {
            picture.setReviewStatus(PictureReviewStatusEnum.PENDING.getValue());
        }
    }

    /**
     * 校验图片操作权限
     *
     * @param loginUser 登录用户
     * @param picture   图片
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        ThrowUtils.throwIf(loginUser == null || picture == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库：仅本人或系统管理员可操作
            if (!loginUser.isAdmin() && !loginUser.getId().equals(picture.getUserId())) {
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
            }
            return;
        }
        // 空间图库：仅空间创建者或系统管理员可操作
        Space space = spaceRepository.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
    }

    /**
     * 编辑图片信息
     *
     * @param editPictureRequest 编辑参数
     * @param loginUser          登录用户
     */
    @Override
    public void editPicture(EditPictureRequest editPictureRequest, User loginUser) {
        ThrowUtils.throwIf(editPictureRequest == null, ErrorCode.PARAMS_ERROR);
        // 1. 构建待更新的图片对象
        Picture picture = PictureAssembler.toPictureEntity(editPictureRequest);
        picture.setTags(JSONUtil.toJsonStr(editPictureRequest.getTags()));
        picture.setEditTime(new Date());
        picture.validPicture();

        // 2. 校验图片存在
        Picture oldPicture = this.getById(editPictureRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 3. 填充审核信息（普通用户编辑后重新进入待审核）
        this.fillReviewParams(picture, loginUser);

        // 4. 更新
        boolean updated = this.updateById(picture);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "图片更新失败，数据库错误");
    }

    /**
     * 通过主色调查询相似图片
     *
     * @param spaceId   空间 id
     * @param color     目标颜色
     * @param loginUser 登录用户
     * @return 相似图片列表
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, Color color, User loginUser) {
        // 1. 查询空间内所有有主色调的图片
        List<Picture> pictureList = pictureRepository.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getMainColor)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        // 2. 按颜色相似度排序，相似度越高越靠前
        return pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getMainColor();
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    try {
                        double similarity = MainColorSimilarUtils.calculateSimilarity(Color.decode(hexColor), color);
                        return -similarity;
                    } catch (NumberFormatException e) {
                        // 历史脏数据无法解析时排到最后
                        return Double.MAX_VALUE;
                    }
                }))
                .limit(COLOR_SEARCH_LIMIT)
                .map(PictureVO::objToVo)
                .toList();
    }

    /**
     * 更新图片信息（管理员自动过审）
     *
     * @param picture   图片
     * @param loginUser 登录用户
     * @return true 表示更新成功
     */
    @Override
    public boolean updatePicture(Picture picture, User loginUser) {
        ThrowUtils.throwIf(picture == null || picture.getId() == null, ErrorCode.PARAMS_ERROR);
        // 1. 校验图片存在
        Picture oldPicture = pictureRepository.getById(picture.getId());
        ThrowUtils.throwIf(ObjUtil.isNull(oldPicture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 2. 填充审核参数
        this.fillReviewParams(picture, loginUser);

        // 3. 更新。
        // 注意：updateById 会忽略 null 字段，导致「重置为待审核」时旧的审核信息残留，
        // 因此这里用 UpdateWrapper 显式把审核相关字段置空。
        LambdaUpdateWrapper<Picture> updateWrapper = new LambdaUpdateWrapper<Picture>()
                .eq(Picture::getId, picture.getId())
                .set(ObjUtil.isNull(picture.getReviewerId()), Picture::getReviewerId, null)
                .set(ObjUtil.isNull(picture.getReviewTime()), Picture::getReviewTime, null)
                .set(ObjUtil.isNull(picture.getReviewMessage()), Picture::getReviewMessage, null);
        return pictureRepository.update(picture, updateWrapper);
    }

    /**
     * 多级缓存分页查询图片 VO
     *
     * @param pictureQueryRequest 查询参数
     * @param userLogin           登录用户
     * @param loader              数据库回源方法
     * @return 图片 VO 分页
     */
    @Override
    public Page<PictureVO> getPictureVOPageUseCache(PictureQueryRequest pictureQueryRequest,
                                                    User userLogin,
                                                    Supplier<Page<PictureVO>> loader) {
        // 1. 生成缓存 key
        String cacheKey = generateCacheKey(pictureQueryRequest);

        // 2. 本地缓存
        String localCache = getLocalCache(cacheKey);
        if (localCache != null) {
            return JSONUtil.toBean(localCache, new TypeReference<Page<PictureVO>>() {
            }, true);
        }

        // 3. Redis 缓存（使用 TypeReference 保留泛型，避免 records 退化为 JSONObject）
        String redisCache = redisUtils.get(cacheKey);
        if (StrUtil.isNotBlank(redisCache)) {
            localCacheManage.putCache(cacheKey, redisCache);
            return JSONUtil.toBean(redisCache, new TypeReference<Page<PictureVO>>() {
            }, true);
        }

        // 4. 回源数据库
        Page<PictureVO> pictureVOPage = loader.get();
        String json = JSONUtil.toJsonStr(pictureVOPage);

        // 5. 回填本地缓存与 Redis（5-10 分钟随机过期，避免缓存雪崩）
        localCacheManage.putCache(cacheKey, json);
        long expireSeconds = RandomUtil.randomLong(60 * 5, 60 * 10);
        redisUtils.set(cacheKey, json, expireSeconds);
        return pictureVOPage;
    }

    /**
     * 统计公共图库/全部空间的用量
     *
     * @param publicGallery true 表示只统计公共图库，false 表示统计全部图片
     * @return 用量统计
     */
    @Override
    public SpaceUsageAnalyzeResponse countUsageAnalyze(boolean publicGallery) {
        // 1. 用 SQL 聚合代替「查全表再在内存里累加」，避免大表 OOM
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("COALESCE(SUM(picSize), 0) AS usedSize", "COUNT(*) AS usedCount");
        if (publicGallery) {
            queryWrapper.isNull("spaceId");
        }

        // 2. 解析聚合结果
        List<Map<String, Object>> resultList = pictureRepository.getBaseMapper().selectMaps(queryWrapper);
        long usedSize = 0L;
        long usedCount = 0L;
        if (CollUtil.isNotEmpty(resultList)) {
            Map<String, Object> row = resultList.get(0);
            usedSize = toLong(row.get("usedSize"));
            usedCount = toLong(row.get("usedCount"));
        }

        // 3. 公共图库没有上限，比例统一返回 null
        SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
        response.setUsedSize(usedSize);
        response.setUsedCount(usedCount);
        response.setMaxSize(null);
        response.setMaxCount(null);
        response.setSizeUsageRatio(null);
        response.setCountUsageRatio(null);
        return response;
    }

    /**
     * 安全地把聚合结果转换为 long
     *
     * @param value 聚合值
     * @return long 值
     */
    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 读取本地缓存
     *
     * @param cacheKey 缓存 key
     * @return 缓存内容，未命中返回 null
     */
    private String getLocalCache(String cacheKey) {
        try {
            String localCache = localCacheManage.getCache(cacheKey);
            return StrUtil.isBlank(localCache) ? null : localCache;
        } catch (Exception e) {
            log.warn("本地缓存查询失败，key={}", cacheKey);
            return null;
        }
    }

    /**
     * 通过请求参数生成缓存 key
     *
     * @param pictureQueryRequest 查询参数
     * @return 缓存 key
     */
    private String generateCacheKey(PictureQueryRequest pictureQueryRequest) {
        String hashKey = DigestUtil.md5Hex(JSONUtil.toJsonStr(pictureQueryRequest));
        return PictureCacheConstant.PICTURE_LIST_PAGE_VO_KEY_PREFIX + hashKey;
    }
}
