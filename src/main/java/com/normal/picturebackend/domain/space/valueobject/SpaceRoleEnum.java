package com.normal.picturebackend.domain.space.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 空间角色枚举
 */
@Getter
@AllArgsConstructor
public enum SpaceRoleEnum {
    ADMIN("管理员","admin"),
    VIEWER("浏览者", "viewer"),
    EDITOR("编辑者", "editor");

    private final String text;

    private final String value;


    /**
     * 根据value拿到枚举
     */
    public static SpaceRoleEnum getEnumByValue(String value) {
        if (value == null){
            return null;
        }

        for (SpaceRoleEnum anEnum : SpaceRoleEnum.values()) {
            if (Objects.equals(anEnum.getValue(), value)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 获得所有枚举的文本列表
     */
    public static List<String> getAllText() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getText)
                .toList();
    }

    /**
     * 获得所有枚举的值列表
     */
    public static List<String> getAllValue() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getValue)
                .toList();
    }



}
