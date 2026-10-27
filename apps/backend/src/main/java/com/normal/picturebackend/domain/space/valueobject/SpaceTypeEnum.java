package com.normal.picturebackend.domain.space.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 空间类型枚举 
 */
@Getter
@AllArgsConstructor
public enum SpaceTypeEnum {
    PRIVATE("私有空间",0),
    TEEM("团队空间", 1);

    private final String text;

    private final int value;


    /**
     * 根据value拿到枚举
     */
    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (value == null){
            return null;
        }

        for (SpaceTypeEnum anEnum : SpaceTypeEnum.values()) {
            if (anEnum.getValue() == value) {
                return anEnum;
            }
        }
        return null;
    }

}
