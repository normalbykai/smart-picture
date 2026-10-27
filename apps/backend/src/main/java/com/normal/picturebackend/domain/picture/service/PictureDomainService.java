package com.normal.picturebackend.domain.picture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.interfaces.dto.picture.EditPictureRequest;
import com.normal.picturebackend.interfaces.dto.picture.PictureQueryRequest;
import com.normal.picturebackend.interfaces.dto.picture.PictureReviewRequest;
import com.normal.picturebackend.interfaces.vo.picture.PictureVO;
import com.normal.picturebackend.interfaces.vo.space.SpaceUsageAnalyzeResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;

/**
 * 图片领域服务。
 *
 * <p>只包含图片自身的业务规则；上传、批量上传、批量编辑等用例编排放在应用层。</p>
 *
 * @author norma
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-04-19 16:54:15
 */
public interface PictureDomainService extends IService<Picture> {

    /**
     * 将图片查询请求转换为查询包装器
     *
     * @param pictureQueryRequest 图片查询请求
     * @return 查询包装器
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 校验图片
     *
     * @param picture 图片信息
     */
    void validPicture(Picture picture);

    /**
     * 转换为 PictureVO 对象
     *
     * @param picture 图片信息
     * @return 图片 VO 信息
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 转换为 PictureVO 对象（兼容旧调用）
     *
     * @param picture 图片信息
     * @param request 请求
     * @return 图片 VO 信息
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核参数
     * @param loginUser            登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充图片审核信息
     *
     * @param picture   图片
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 校验图片权限
     *
     * @param loginUser 登录用户
     * @param picture   图片
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 编辑图片
     *
     * @param editPictureRequest 编辑参数
     * @param loginUser          登录用户
     */
    void editPicture(EditPictureRequest editPictureRequest, User loginUser);

    /**
     * 获取主色调相似的图片集合
     *
     * @param spaceId   空间 id
     * @param color     目标颜色
     * @param loginUser 登录用户
     * @return 相似图片列表
     */
    List<PictureVO> searchPictureByColor(Long spaceId, Color color, User loginUser);

    /**
     * 更新图片信息
     *
     * @param picture   图片
     * @param loginUser 登录用户
     * @return true 表示更新成功
     */
    boolean updatePicture(Picture picture, User loginUser);

    /**
     * 获取图片封装类信息（多级缓存）
     *
     * @param pictureQueryRequest 查询参数
     * @param userLogin           登录用户
     * @param loader              数据库回源方法
     * @return 图片 VO 分页
     */
    Page<PictureVO> getPictureVOPageUseCache(PictureQueryRequest pictureQueryRequest,
                                             User userLogin,
                                             Supplier<Page<PictureVO>> loader);

    /**
     * 统计公共图库/全部空间的使用情况
     *
     * @param publicGallery true 表示只统计公共图库，false 表示统计全部图片
     * @return 分析封装类
     */
    SpaceUsageAnalyzeResponse countUsageAnalyze(boolean publicGallery);
}
