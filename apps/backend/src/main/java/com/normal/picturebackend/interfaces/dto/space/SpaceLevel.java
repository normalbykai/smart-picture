package com.normal.picturebackend.interfaces.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class SpaceLevel {

    private int value;

    private String text;

    private long maxCount;

    private long maxSize;
}
