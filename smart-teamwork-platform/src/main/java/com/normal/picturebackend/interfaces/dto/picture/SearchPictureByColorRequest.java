package com.normal.picturebackend.interfaces.dto.picture;

import lombok.Data;

@Data
public class SearchPictureByColorRequest {

    private Long spaceId;

    private String mainColor;
}
