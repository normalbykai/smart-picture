package com.normal.picturebackend.domain.space.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;
import com.normal.picturebackend.domain.space.valueobject.SpaceLevelEnum;
import com.normal.picturebackend.domain.space.valueobject.SpaceTypeEnum;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * 空间
 * @TableName space
 */
@TableName(value ="space")
@Data
public class Space implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 空间类型: 0-私有 1-团队
     */
    private Integer spaceType;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 校验空间参数
     *
     * @param add 添加参数
     */
    public void validate(boolean add) {

        // 对象中获取数据
        String spaceName = this.getSpaceName();
        Integer spaceLevel = this.getSpaceLevel();
        Integer spaceType = this.getSpaceType();

        // 空间级别枚举
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);

        // 空间类型枚举
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);


        // 如果是创建空间 则需要判断一下空间名字 和 空间级别
        if (add) {
            // 校验参数 - 必须要填写空间参数
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名不能为空");
            }

            if (spaceLevelEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级错误");
            }

            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
        }

        // 空间类型判断
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型错误");
        }

        // 修改数据时，空间名称进行校验：最多 36 个字符
        // 注意必须先判断非空再取 length，否则空名称会抛空指针
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 36) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名过长");
        }

        // 修改数据时， 空间级别校验
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
    }


    /**
     * 填充空间级别信息 通过空间中的空间级别填充 默认最大容量/使用....
     */
    public void fillSpaceBySpaceLevel() {
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(this.getSpaceLevel());

        if (enumByValue != null) {
            // 如果传入的这个空间对象已经存在最大容量 或 最大数量 则不进行设置
            if (this.getMaxCount() == null) {
                // 设置 默认该级别的空间最大数量
                this.setMaxCount(enumByValue.getMaxCount());
            }

            if (this.getMaxSize() == null) {
                // 设置 默认该级别的空间最大容量
                this.setMaxSize(enumByValue.getMaxSize());
            }

        }
    }

    /**
     * 校验传入的userId 是否是当前space 的创建者
     */
    public boolean isSpaceCreator(Long userId) {
        return Objects.equals(userId, this.getUserId());
    }

    /**
     * 通过userId校验该空间不是这个创建者
     *
     * @param userId
     * @return
     */
    public boolean isNotSpaceCreator(Long userId) {
        return !this.isSpaceCreator(userId);
    }
}