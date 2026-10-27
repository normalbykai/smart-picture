package com.normal.picturebackend.interfaces.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.normal.picturebackend.application.service.PictureApplicationService;
import com.normal.picturebackend.application.service.UserApplicationService;
import com.normal.picturebackend.infrastructure.annotation.AuthCheck;
import com.normal.picturebackend.infrastructure.api.imagesearch.ImageSearchApiFacade;
import com.normal.picturebackend.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.normal.picturebackend.infrastructure.common.BaseResponse;
import com.normal.picturebackend.infrastructure.common.DeleteRequest;
import com.normal.picturebackend.infrastructure.common.ResultUtils;
import com.normal.picturebackend.shared.auth.valueobject.SpaceUserPermissionConstant;
import com.normal.picturebackend.domain.user.valueobject.UserConstant;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.infrastructure.exception.ThrowUtils;
import com.normal.picturebackend.shared.auth.StpKit;
import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.interfaces.assembler.PictureAssembler;
import com.normal.picturebackend.interfaces.dto.picture.*;
import com.normal.picturebackend.interfaces.vo.picture.PictureVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * 图片控制层
 */
@RestController
@Slf4j
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureApplicationService pictureApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 图片上传 可重复
     *
     * @param multipartFile        图片文件
     * @param pictureUploadRequest 上传图片请求
     * @param request              用户服务请求
     * @return 图片VO
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SaCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD, type = StpKit.SPACE_TYPE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart(value = "file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        // 1. 获得用户登录信息
        User userLogin = userApplicationService.getUserLogin(request);

        // 2. 开始上传图片逻辑
        PictureVO pictureVO = pictureApplicationService.uploadPicture(multipartFile, pictureUploadRequest, userLogin);

        // 3. 返回上传图片后的图片信息
        return ResultUtils.success(pictureVO);
    }

    /**
     * 图片上传 可重复 (按照URL地址上传)
     *
     * @param pictureUploadRequest 上传图片请求 (URL地址)
     * @param request              用户服务请求
     * @return 图片VO
     */
    @PostMapping(value = "/upload/url")
    @SaCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD, type = StpKit.SPACE_TYPE)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                      HttpServletRequest request) {
        // 1. 获得登录的用户信息
        User userLogin = userApplicationService.getUserLogin(request);

        // 2. 获取待上传图片的URL地址
        String fileUrl = pictureUploadRequest.getFileUrl();

        // 3. 开始上传图片
        PictureVO pictureVO = pictureApplicationService.uploadPicture(fileUrl, pictureUploadRequest, userLogin);

        // 返回已上传完成的图片信息
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     *
     * @param deleteRequest 要删除图片的id
     * @param request       登录用户请求 会读取本地session获取登录信息
     * @return true 删除成功
     */
    @PostMapping("/delete")
    @SaCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE, type = StpKit.SPACE_TYPE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User currentLoginUser = userApplicationService.getUserLogin(request);
        return ResultUtils.success(pictureApplicationService.deletePicture(deleteRequest, currentLoginUser));
    }


    /**
     * 更新图片信息 （用户提交审核状态为待处理，管理员提交审核状态自动成功）
     *
     * @param pictureUpdateRequest 图片更新请求
     * @return true 为更新成功
     */
    @PostMapping("/update")
    @SaCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD, type = StpKit.SPACE_TYPE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 1. 获取当前登录的用户信息
        User loginUser = userApplicationService.getUserLogin(request);

        // 2. 构建图片更新实体
        Picture picture = PictureAssembler.toPictureEntity(pictureUpdateRequest);
        // 将list 转换为 string-json字符串
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));

        // 3. 校验参数
        picture.validPicture();

        // 4. 更新图片, 返回更新状态
        return ResultUtils.success(pictureApplicationService.updatePicture(picture, loginUser));
    }


    /**
     * 获取图片信息 （仅管理员）
     *
     * @param id 图片id
     * @return picture图片信息
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/get")
    public BaseResponse<Picture> getPictureById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查找数据库
        Picture picture = pictureApplicationService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 获取图片封装
     *
     * @param id      图片id
     * @param request 登录用户的信息请求
     * @return 图片封装类VO
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        // 公共图库允许匿名浏览；未登录时不抛出 401，而是以「无权限用户」继续查询
        User loginUser = null;
        try {
            loginUser = userApplicationService.getUserLogin(request);
        } catch (BusinessException e) {
            if (e.getCode() != ErrorCode.NOT_LOGIN_ERROR.getCode()) {
                throw e;
            }
        }
        return ResultUtils.success(pictureApplicationService.getPictureVOById(id, loginUser));
    }

    /**
     * 分页查询图片列表 （仅管理员）
     *
     * @param pictureQueryRequest 查询参数请求
     * @return 图片分页列表
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page")
    public BaseResponse<Page<Picture>> getPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取查询包装器
        QueryWrapper<Picture> queryWrapper = pictureApplicationService.getQueryWrapper(pictureQueryRequest);
        // 分页查询
        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(pictureQueryRequest.getCurrentNum(), pictureQueryRequest.getPageSize()), queryWrapper);
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页查询图片封装类
     *
     * @param pictureQueryRequest 查询参数请求
     * @return 图片VO分页列表
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> getPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 1. 获取图片VO分页列表
        Page<Picture> pictureVOByPage = pictureApplicationService.getPictureVOByPage(pictureQueryRequest, null);
        // 2. 返回
        return ResultUtils.success(pictureApplicationService.getPictureVOPage(pictureVOByPage, request));
    }

    /**
     * 分页查询图片VO列表 使用缓存
     *
     * @param pictureQueryRequest 查询参数请求
     * @return 图片VO分页列表
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> getPictureVOByPageUseCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {

        // 1. 获取当前登录用户
        User userLogin = userApplicationService.getUserLogin(request);

        // 2. 多级缓存查询 (本地缓存 -> 分布式缓存 -> 数据库)
        Page<PictureVO> result = pictureApplicationService.getPictureVOByPageUseCache(pictureQueryRequest, userLogin);
        return ResultUtils.success(result);
    }

    /**
     * 编辑图片 用户使用
     *
     * @param editPictureRequest 编辑图片请求
     * @param httpServletRequest 用户服务请求
     * @return true 为编辑成功
     */
    @PostMapping("/edit")
    @SaCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT, type = StpKit.SPACE_TYPE)
    public BaseResponse<Boolean> editPicture(@Valid @RequestBody EditPictureRequest editPictureRequest, HttpServletRequest httpServletRequest) {
        if (editPictureRequest == null || editPictureRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 当前登录用户
        User loginUser = userApplicationService.getUserLogin(httpServletRequest);

        // 编辑图片
        pictureApplicationService.editPicture(editPictureRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 分类和标签列表查询
     *
     * @return 分页和标签列表
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {

        PictureTagCategory pictureTagCategory = new PictureTagCategory();

        // 自定义标签
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");

        // 自定义分类
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报", "头像");

        pictureTagCategory.setCategoryList(categoryList);

        pictureTagCategory.setTagList(tagList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片 (仅管理员)
     *
     * @param pictureReviewRequest 审核请求参数
     * @param httpServletRequest   请求信息
     * @return true
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doReviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest httpServletRequest) {
        // 1. 参数校验
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);

        // 2.图片审核处理
        User userLogin = userApplicationService.getUserLogin(httpServletRequest);
        pictureApplicationService.doPictureReview(pictureReviewRequest, userLogin);
        return ResultUtils.success(true);
    }


    /**
     * 批量上传请求 (仅管理员)
     *
     * @param pictureUploadByBatchRequest 图片批量上传请求
     * @param httpServletRequest          请求信息
     * @return 上传成功的数量
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest httpServletRequest) {
        // 1. 参数校验
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);

        // 2.图片审核处理
        User userLogin = userApplicationService.getUserLogin(httpServletRequest);
        Integer uploadNumber = pictureApplicationService.uploadPictureByBatch(pictureUploadByBatchRequest, userLogin);
        return ResultUtils.success(uploadNumber);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/picture_search")
    public BaseResponse<List<ImageSearchResult>> imageSearch(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        // 1. 参数校验
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);

        // 2. 查询数据库URL地址
        Picture oldPicture = pictureApplicationService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 以图搜图开始
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImage(oldPicture.getOriginUrl());
        return ResultUtils.success(imageSearchResults);
    }


    /**
     * 主色调搜索
     */
    @PostMapping("/search/color")
    @SaCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW, type = StpKit.SPACE_TYPE)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getUserLogin(request);

        // 2. 获取主色调参数 主色调传入的参数可能为空 - 并且解析为Color对象也可能为空
        String mainColor = searchPictureByColorRequest.getMainColor();
        ThrowUtils.throwIf(StrUtil.isBlank(mainColor), ErrorCode.PARAMS_ERROR);
        Color color = Color.decode(mainColor);

        // 3. 获取 主色调相对的图片列表数据
        List<PictureVO> pictureVOList = pictureApplicationService.searchPictureByColor(searchPictureByColorRequest.getSpaceId(), color, loginUser);
        return ResultUtils.success(pictureVOList);
    }


    /**
     * 批量修改私人空间中图片的数据
     *
     * @param pictureEditBatchRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/edit/batch")
    @SaCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT, type = StpKit.SPACE_TYPE)
    public BaseResponse<Boolean> editPictureBatch(@RequestBody PictureEditBatchRequest pictureEditBatchRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(pictureEditBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getUserLogin(httpServletRequest);
        // TODO 数据量少能正常完成业务 - 可以优化一下 采用线程池异步分批处理避免长事务
        return ResultUtils.success(pictureApplicationService.editPictureBatch(pictureEditBatchRequest, loginUser));
    }
}












