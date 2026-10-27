package com.normal.picturebackend.domain.picture.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 图片审核状态枚举 审核状态：0-待审核; 1-通过; 2-拒绝
 */
@Getter
@AllArgsConstructor
public enum PictureReviewStatusEnum {
    PENDING("待审核",0),
    PASS("通过", 1),
    REJECT("拒绝",2);

    private final String text;

    private final int value;


    /**
     * 根据value拿到枚举
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value) {
        if (value == null){
            return null;
        }

        for (PictureReviewStatusEnum anEnum : PictureReviewStatusEnum.values()) {
            if (anEnum.getValue() == value) {
                return anEnum;
            }
        }
        return null;
    }

}
