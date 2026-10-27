package com.normal.picturebackend.interfaces.vo.spaceuser;

import com.normal.picturebackend.domain.space.entity.SpaceUser;
import com.normal.picturebackend.interfaces.vo.space.SpaceVO;
import com.normal.picturebackend.interfaces.vo.user.UserVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 空间用户关联 响应对象
 */
@Data
public class SpaceUserVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色: viewer/editor/admin
     */
    private String spaceRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 空间信息
     */
    private SpaceVO space;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 将SpaceUserVO 转换 为SpaceUser
     */
    public static SpaceUser voToObj(SpaceUserVO spaceUserVO) {
        if (spaceUserVO == null) {
            return null;
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserVO, spaceUser);
        return spaceUser;
    }

    /**
     * 将对象转换为封装类
     */
    public static SpaceUserVO objToVo(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }
        SpaceUserVO spaceUserVO = new SpaceUserVO();
        BeanUtils.copyProperties(spaceUser, spaceUserVO);
        return spaceUserVO;
    }

    @Serial
    private static final long serialVersionUID = 1L;
}
