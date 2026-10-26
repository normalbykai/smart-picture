package com.normal.picturebackend.domain.user.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserRoleEnum {

    ADMIN("管理员", "admin"),
    USER("用户", "user");

    private final String text;

    private final String value;


    /**
     * 根据value拿到枚举
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (value == null){
            return null;
        }

        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
