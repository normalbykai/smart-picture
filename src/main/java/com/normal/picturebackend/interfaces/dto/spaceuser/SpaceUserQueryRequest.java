package com.normal.picturebackend.interfaces.dto.spaceuser;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SpaceUserQueryRequest implements Serializable {

    /**
     * ID
     */
    private Long id;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 空间角色: viewer/editor/admin
     */
    private String spaceRole;

    @Serial
    private static final long serialVersionUID = 1L;



}
