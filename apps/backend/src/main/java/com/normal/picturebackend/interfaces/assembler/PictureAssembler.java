package com.normal.picturebackend.interfaces.assembler;

import cn.hutool.core.bean.BeanUtil;
import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.interfaces.dto.picture.EditPictureRequest;
import com.normal.picturebackend.interfaces.dto.picture.PictureReviewRequest;
import com.normal.picturebackend.interfaces.dto.picture.PictureUpdateRequest;

public class PictureAssembler {

    public static Picture toPictureEntity(PictureReviewRequest pictureReviewRequest) {
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, picture);
        return picture;
    }


    public static Picture toPictureEntity(EditPictureRequest editPictureRequest) {
        Picture picture = new Picture();
        BeanUtil.copyProperties(editPictureRequest, picture);
        return picture;
    }

    public static Picture toPictureEntity(PictureUpdateRequest pictureUpdateRequest) {
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        return picture;
    }
}
