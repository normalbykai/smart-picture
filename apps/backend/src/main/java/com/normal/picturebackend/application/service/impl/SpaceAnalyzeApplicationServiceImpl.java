package com.normal.picturebackend.application.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.normal.picturebackend.application.service.SpaceAnalyzeApplicationService;
import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.picture.service.PictureDomainService;
import com.normal.picturebackend.domain.space.entity.Space;
import com.normal.picturebackend.domain.space.service.SpaceDomainService;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.infrastructure.exception.ThrowUtils;
import com.normal.picturebackend.interfaces.dto.space.SpaceAnalyzeRequest;
import com.normal.picturebackend.interfaces.dto.space.SpaceCategoryAnalyzeRequest;
import com.normal.picturebackend.interfaces.dto.space.SpaceRankAnalyzeRequest;
import com.normal.picturebackend.interfaces.dto.space.SpaceSizeAnalyzeRequest;
import com.normal.picturebackend.interfaces.dto.space.SpaceTagAnalyzeRequest;
import com.normal.picturebackend.interfaces.dto.space.SpaceUserAnalyzeRequest;
import com.normal.picturebackend.interfaces.vo.space.SpaceCategoryAnalyzeResponse;
import com.normal.picturebackend.interfaces.vo.space.SpaceSizeAnalyzeResponse;
import com.normal.picturebackend.interfaces.vo.space.SpaceTagAnalyzeResponse;
import com.normal.picturebackend.interfaces.vo.space.SpaceUsageAnalyzeResponse;
import com.normal.picturebackend.interfaces.vo.space.SpaceUserAnalyzeResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 空间分析应用服务实现。
 *
 * <p>权限约定：</p>
 * <ul>
 *     <li>「全部空间 / 公共图库」的统计仅系统管理员可见，服务层必须校验，不能只依赖注解；</li>
 *     <li>具体私人/团队空间的访问权限由控制器上的 {@code @SaSpaceCheckPermission} 保证；</li>
 *     <li>统计尽量下推到 SQL 聚合，避免把整表数据读进内存。</li>
 * </ul>
 */
@Service(value = "spaceAnalyzeApplicationServiceImpl")
@Slf4j
public class SpaceAnalyzeApplicationServiceImpl implements SpaceAnalyzeApplicationService {

    /**
     * 空间排行榜允许的最大条数
     */
    private static final int MAX_RANK_TOP_N = 100;

    @Resource
    private PictureDomainService pictureDomainService;

    @Resource
    private SpaceDomainService spaceDomainService;

    /**
     * 校验分析范围权限。
     *
     * <p>全空间/公共图库统计包含所有用户的数据，仅管理员可看；
     * 指定 spaceId 的统计由控制器的空间权限注解负责。</p>
     *
     * @param request   分析请求
     * @param loginUser 登录用户
     */
    private void checkAnalyzeScopeAuth(SpaceAnalyzeRequest request, User loginUser) {
        if (request.isQueryAll() || request.isQueryPublic()) {
            ThrowUtils.throwIf(loginUser == null || !loginUser.isAdmin(),
                    ErrorCode.NOT_AUTH_ERROR, "仅管理员可以查看全空间/公共图库统计");
            return;
        }
        Long spaceId = request.getSpaceId();
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "空间 id 不能为空");
    }

    /**
     * 空间使用情况分析
     *
     * @param request   分析请求
     * @param loginUser 登录用户
     * @return 使用情况
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyzeResult(SpaceAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        checkAnalyzeScopeAuth(request, loginUser);

        // 全空间/公共图库统计
        if (request.isQueryAll() || request.isQueryPublic()) {
            // queryAll 与 queryPublic 同时为 true 时，以 queryAll 为准
            if (request.isQueryAll()) {
                request.setQueryPublic(false);
            }
            return pictureDomainService.countUsageAnalyze(request.isQueryPublic());
        }

        // 具体空间：直接从空间记录读取累计值，避免全表扫描
        return spaceDomainService.countSpaceUsageAnalyze(request.getSpaceId());
    }

    /**
     * 分类分析
     *
     * @param request   分析请求
     * @param loginUser 登录用户
     * @return 分类统计
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyzeResult(SpaceCategoryAnalyzeRequest request,
                                                                           User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        checkAnalyzeScopeAuth(request, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeRequestWrapper(request, queryWrapper);
        queryWrapper.select("category as category", "count(*) AS count", "sum(picSize) as totalSize")
                .groupBy("category");

        List<Map<String, Object>> maps = pictureDomainService.getBaseMapper().selectMaps(queryWrapper);
        return maps.stream().map(row -> {
            String category = row.get("category") != null ? row.get("category").toString() : "未分类";
            long count = toLong(row.get("count"));
            long totalSize = toLong(row.get("totalSize"));
            return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
        }).toList();
    }

    /**
     * 标签分析
     *
     * @param request   分析请求
     * @param loginUser 登录用户
     * @return 标签统计
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyzeResult(SpaceTagAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        checkAnalyzeScopeAuth(request, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeRequestWrapper(request, queryWrapper);
        queryWrapper.select("tags");

        // 1. 取出所有非空标签 JSON 字符串
        List<String> tagsJsonList = pictureDomainService.getBaseMapper()
                .selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .toList();

        // 2. 解析 JSON 数组并统计每个标签出现的次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        // 3. 按次数降序返回
        return tagCountMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 图片大小分段分析
     *
     * @param request   分析请求
     * @param loginUser 登录用户
     * @return 大小分布
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyzeResult(SpaceSizeAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        checkAnalyzeScopeAuth(request, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeRequestWrapper(request, queryWrapper);
        queryWrapper.select("picSize");

        List<Long> picSizes = pictureDomainService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> toLong(size))
                .toList();

        // 使用有序 Map 保证返回顺序稳定
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizes.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizes.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizes.stream().filter(size -> size >= 500 * 1024 && size < 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizes.stream().filter(size -> size >= 1024 * 1024).count());

        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 用户上传行为分析
     *
     * @param request   分析请求
     * @param loginUser 登录用户
     * @return 按时间维度的上传数量
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyzeResult(SpaceUserAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        checkAnalyzeScopeAuth(request, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        String userId = request.getUserId();
        queryWrapper.eq(StrUtil.isNotBlank(userId), "userId", userId);
        fillAnalyzeRequestWrapper(request, queryWrapper);

        // 时间维度决定分组表达式；未传时给出明确错误，避免 switch NPE
        String timeDimension = request.getTimeDimension();
        ThrowUtils.throwIf(StrUtil.isBlank(timeDimension), ErrorCode.PARAMS_ERROR, "时间维度不能为空");
        switch (timeDimension) {
            case "day" -> queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') as period", "count(*) AS count");
            case "week" -> queryWrapper.select("YEARWEEK(createTime) as period", "count(*) AS count");
            case "month" -> queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') as period", "count(*) AS count");
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        queryWrapper.groupBy("period").orderByAsc("period");

        List<Map<String, Object>> list = pictureDomainService.getBaseMapper().selectMaps(queryWrapper);
        return list.stream()
                .map(row -> new SpaceUserAnalyzeResponse(
                        String.valueOf(row.get("period")),
                        toLong(row.get("count"))))
                .toList();
    }

    /**
     * 空间使用排行（仅管理员）
     *
     * @param request   分析请求
     * @param loginUser 登录用户
     * @return 空间列表
     */
    @Override
    public List<Space> getSpaceRankAnalyzeResult(SpaceRankAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null || !loginUser.isAdmin(),
                ErrorCode.NOT_AUTH_ERROR, "无权限查看空间排行");

        // 限制 topN 范围，避免恶意传入超大值
        int topN = request.getTopN() == null ? 10 : request.getTopN();
        ThrowUtils.throwIf(topN <= 0 || topN > MAX_RANK_TOP_N,
                ErrorCode.PARAMS_ERROR, "排行数量需在 1-" + MAX_RANK_TOP_N + " 之间");

        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + topN);
        return spaceDomainService.list(queryWrapper);
    }

    /**
     * 填充分析范围查询条件
     *
     * @param request      分析请求
     * @param queryWrapper 查询包装器
     */
    private void fillAnalyzeRequestWrapper(SpaceAnalyzeRequest request, QueryWrapper<Picture> queryWrapper) {
        if (request.isQueryAll()) {
            // 全部图片，不追加条件
            return;
        }
        if (request.isQueryPublic()) {
            // 公共图库：spaceId 为空
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = request.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询条件错误");
    }

    /**
     * 安全地把统计结果转换为 long
     *
     * @param value 统计值
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
}
