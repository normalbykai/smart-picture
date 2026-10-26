package com.normal.picturebackend.interfaces.dto.spaceuser;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 空间用户编辑请求
 */
@Data
public class SpaceUserEditRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间角色
     */
    private String spaceRole;

    @Serial
    private static final long serialVersionUID = 1L;
}
