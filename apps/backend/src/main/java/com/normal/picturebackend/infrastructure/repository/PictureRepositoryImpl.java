package com.normal.picturebackend.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.normal.picturebackend.domain.picture.entity.Picture;
import com.normal.picturebackend.domain.picture.repository.PictureRepository;
import com.normal.picturebackend.infrastructure.mapper.PictureMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {
}
