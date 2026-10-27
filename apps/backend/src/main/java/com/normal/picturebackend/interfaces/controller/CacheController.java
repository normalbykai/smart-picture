package com.normal.picturebackend.interfaces.controller;

import com.normal.picturebackend.domain.picture.valueobject.PictureCacheConstant;
import com.normal.picturebackend.domain.user.valueobject.UserConstant;
import com.normal.picturebackend.infrastructure.annotation.AuthCheck;
import com.normal.picturebackend.infrastructure.common.BaseResponse;
import com.normal.picturebackend.infrastructure.common.RedisUtils;
import com.normal.picturebackend.infrastructure.common.ResultUtils;
import com.normal.picturebackend.infrastructure.manager.cache.LocalCacheManage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缓存管理接口（仅管理员）。
 *
 * <p>只清理图片列表缓存，不能使用空前缀清理全部 Redis，
 * 否则会把登录 Session、限流计数等无关数据一起删掉。</p>
 */
@RestController
@Slf4j
@RequestMapping("/admin/cache")
@Tag(name = "缓存接口", description = "缓存的控制层")
public class CacheController {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private LocalCacheManage localCacheManage;

    /**
     * 清空图片列表缓存（本地 + Redis）
     *
     * @return true 表示成功
     */
    @Operation(summary = "清空图片列表缓存", description = "清空本地缓存与 Redis 中的图片列表缓存")
    @PostMapping("/clear/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> clearCacheAll() {
        localCacheManage.clearCache();
        redisUtils.deleteByKeyPrefix(PictureCacheConstant.PICTURE_LIST_PAGE_VO_KEY_PREFIX);
        return ResultUtils.success(true);
    }

    /**
     * 清空首页图片列表缓存（保留接口兼容旧前端）
     *
     * @return true 表示成功
     */
    @Operation(summary = "清空首页缓存记录", description = "清空本地缓存与图片列表 Redis 缓存")
    @PostMapping("/clear/home")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> clearCacheHome() {
        localCacheManage.clearCache();
        // 复用统一常量，避免前缀写错导致缓存清不掉
        redisUtils.deleteByKeyPrefix(PictureCacheConstant.PICTURE_LIST_PAGE_VO_KEY_PREFIX);
        return ResultUtils.success(true);
    }
}
