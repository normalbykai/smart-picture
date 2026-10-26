package com.normal.picturebackend.domain.user.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.normal.picturebackend.domain.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户仓库
 */
@Mapper
public interface UserRepository extends IService<User> {
}
