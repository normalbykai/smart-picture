package com.normal.picturebackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.infrastructure.common.DeleteRequest;
import com.normal.picturebackend.interfaces.dto.picture.*;
import com.normal.picturebackend.interfaces.vo.picture.PictureVO;
import jakarta.servlet.http.HttpServletRequest;

import java.awt.*;
import java.util.List;

/**
 * 图片服务类
 *
 * @author norma
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-04-19 16:54:15
 */
public interface PictureApplicationService extends IService<Picture> {

    /**
     * 将图片查询请求 转换为 查询包装器
     *
     * @param pictureQueryRequest 图片查询请求
     * @return 查询包装器
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);


    /**
     * 转换为pictureVO 对象
     *
     * @param picture 图片信息
     * @return 图片VO信息
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 将图片分页对象转换为分页VO对象
     *
     * @param picturePage 分页的图片对象
     * @param request     用户请求参数
     * @return 分页图片VO对象
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 图片上传 （可重复上传）
     *
     * @param inputSource          输入源 可以是 MultipartFile 或者 url
     * @param pictureUploadRequest 图片上传参数 - ID
     * @param user                 登录的用户
     * @return 图片VO
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User user);

    /**
     * 图片审核 （审核不通过会抛出异常）
     *
     * @param pictureReviewRequest 图片审核参数
     * @param loginUser            登录的用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充图片审核信息
     *
     * @param picture   图片
     * @param loginUser 登录的用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 图片批量上传通过 关键词进行网络传输上传图片
     * @param pictureUploadByBatchRequest 图片批量上传的请求
     * @param loginUser 登录的用户
     * @return
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 编辑图片
     * @param editPictureRequest
     * @param loginUser
     */
    void editPicture(EditPictureRequest editPictureRequest, User loginUser);

    /**
     * 获取主色调相似集合数据
     * @param spaceId
     * @param color
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, Color color, User loginUser);

    /**
     * 批量编辑私人空间中 - 批量图片的信息
     * @param pictureEditBatchRequest
     * @param loginUser
     * @return
     */
    boolean editPictureBatch(PictureEditBatchRequest pictureEditBatchRequest, User loginUser);

    /**
     * 图片删除
     *
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    Boolean deletePicture(DeleteRequest deleteRequest, User loginUser);

    /**
     * 更新图片
     * @param picture
     * @param loginUser
     */
    boolean updatePicture(Picture picture, User loginUser);

    /**
     * 获取图片视图对象
     *
     * @param id
     * @param loginUser
     * @return
     */
    PictureVO getPictureVOById(long id, User loginUser);

    /**
     * 获取图片分页视图对象
     *
     * @param pictureQueryRequest
     * @param loginUser
     */
    Page<Picture> getPictureVOByPage(PictureQueryRequest pictureQueryRequest, User loginUser);

    /**
     * 多级缓存分页查询图片封装类 (本地缓存 -> 分布式缓存 -> 数据库)
     *
     * @param pictureQueryRequest
     * @param userLogin
     * @return
     */
    Page<PictureVO> getPictureVOByPageUseCache(PictureQueryRequest pictureQueryRequest, User userLogin);
}
