package com.normal.picturebackend.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.user.repository.UserRepository;
import com.normal.picturebackend.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Repository;


/**
 * 用户数据访问层实现类
 */
@Repository
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
}
