package com.normal.picturebackend.shared.auth.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class SpaceUserRole implements Serializable {
    /**
     * 角色键
     */
    private String key;

    /**
     * 角色名字
     */
    private String name;

    /**
     * 角色权限
     */
    private List<String> permissions;

    /**
     * 角色描述
     */
    private String description;

    @Serial
    private static final long serialVersionUID = 1L;
}
