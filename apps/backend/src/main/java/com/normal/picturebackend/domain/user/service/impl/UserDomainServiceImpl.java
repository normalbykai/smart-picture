package com.normal.picturebackend.domain.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.user.repository.UserRepository;
import com.normal.picturebackend.domain.user.service.UserDomainService;
import com.normal.picturebackend.domain.user.valueobject.UserConstant;
import com.normal.picturebackend.domain.user.valueobject.UserRoleEnum;
import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import com.normal.picturebackend.infrastructure.exception.ThrowUtils;
import com.normal.picturebackend.infrastructure.mapper.UserMapper;
import com.normal.picturebackend.infrastructure.security.PasswordService;
import com.normal.picturebackend.infrastructure.utils.SortFieldUtils;
import com.normal.picturebackend.interfaces.dto.user.UserQueryRequest;
import com.normal.picturebackend.interfaces.vo.user.UserLoginVO;
import com.normal.picturebackend.interfaces.vo.user.UserVO;
import com.normal.picturebackend.shared.auth.StpKit;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 用户领域服务实现。
 *
 * <p>负责用户注册、登录、登录态解析、密码加密与用户查询等核心逻辑。
 * 登录态只保存用户 id（不再保存整个 User 对象），避免密码哈希进入 Session/Redis。</p>
 *
 * @author norma
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-04-16 23:38:53
 */
@Service
@Slf4j
public class UserDomainServiceImpl extends ServiceImpl<UserMapper, User> implements UserDomainService {

    /**
     * 用户表允许排序的字段白名单（防止 ORDER BY 注入）
     */
    private static final Set<String> USER_SORT_FIELDS =
            Set.of("id", "userAccount", "userName", "userRole", "createTime", "updateTime");

    /**
     * 用户默认密码（管理员创建用户时使用）
     */
    private static final String DEFAULT_PASSWORD = "123456";

    @Resource
    private UserRepository userRepository;

    @Resource
    private PasswordService passwordService;

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 重复密码
     * @return 用户id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验用户是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        long count = userRepository.getBaseMapper().selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已存在");
        }
        // 2. 使用 BCrypt 加密密码（自带随机盐）
        String encryptPassword = passwordService.encode(userPassword);
        // 3. 持久化数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无昵称");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean insertState = userRepository.save(user);
        if (!insertState) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败， 数据库错误");
        }
        return user.getId();
    }

    /**
     * 用户登录。
     *
     * <p>先按账号查询用户，再用 {@link PasswordService} 校验密码；
     * 如果命中的是历史 MD5 密码，会在登录成功后自动升级为 BCrypt。</p>
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      http 请求，用于写入 Session 登录态
     * @return 用户脱敏后的信息
     */
    @Override
    public UserLoginVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 按账号查询用户（不再把密码拼进 SQL，避免时序/注入问题）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 2. 统一提示「用户不存在或密码错误」，避免账号枚举
        if (user == null || !passwordService.matches(userPassword, user.getUserPassword())) {
            log.info("用户登录失败，账号或密码不匹配: {}", userAccount);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        // 3. 历史 MD5 密码在登录成功后自动升级为 BCrypt
        upgradeLegacyPasswordIfNeeded(user, userPassword);

        // 4. 登录态只保存用户 id，密码哈希不再进入 Session / Redis
        Long userId = user.getId();
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, userId);

        // 5. 同步 Sa-Token 的空间权限登录态
        StpKit.SPACE.login(userId);
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, userId);
        return this.getUserLoginVO(user);
    }

    /**
     * 获取当前登录的用户信息
     *
     * @param request HttpRequest
     * @return 登录的用户信息（从数据库读取，保证权限/角色实时生效）
     */
    @Override
    public User getUserLogin(HttpServletRequest request) {
        // 1. 从 Session 中取出登录态（只存了用户 id）
        Object state = request.getSession().getAttribute(USER_LOGIN_STATE);
        Long userId = resolveUserId(state);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 2. 通过 id 查询数据库，避免使用过期缓存导致权限不生效
        User currentUser = userRepository.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 用户注销登录：同时清理 Session 与 Sa-Token 登录态
     *
     * @param request HttpRequest
     * @return 注销成功与否
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object state = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (state == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 1. 移除 Session 登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        // 2. 同步注销 Sa-Token，否则旧 token 仍然可以访问受保护接口
        try {
            StpKit.SPACE.logout();
        } catch (Exception e) {
            log.warn("注销 Sa-Token 登录态失败: {}", e.getMessage());
        }
        return true;
    }

    /**
     * 将用户对象转换为脱敏后的 VO
     *
     * @param user 用户信息
     * @return 脱敏后的用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 批量转换为脱敏后的 VO
     *
     * @param users 用户列表
     * @return 脱敏后的用户列表
     */
    @Override
    public List<UserVO> getUserListVO(List<User> users) {
        if (CollUtil.isEmpty(users)) {
            return new ArrayList<>();
        }
        return users.stream()
                .map(this::getUserVO)
                .toList();
    }

    /**
     * 密码加密（新密码统一使用 BCrypt）
     *
     * @param passwd 明文密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String passwd) {
        return passwordService.encode(passwd);
    }

    /**
     * 获取用户查询包装器
     *
     * @param queryRequest 用户查询请求
     * @return 查询包装器
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest queryRequest) {
        // 1. 校验参数
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        // 2. 拿到参数的值
        Long id = queryRequest.getId();
        String userAccount = queryRequest.getUserAccount();
        String userName = queryRequest.getUserName();
        String userProfile = queryRequest.getUserProfile();
        String userRole = queryRequest.getUserRole();

        // 3. 构建 QueryWrapper
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Objects.nonNull(id), "id", id);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);

        // 4. 排序字段走白名单，非法字段回退到创建时间
        String sortField = SortFieldUtils.resolve(queryRequest.getSortField(), USER_SORT_FIELDS, "createTime");
        queryWrapper.orderBy(true, SortFieldUtils.isAsc(queryRequest.getSortOrder()), sortField);
        return queryWrapper;
    }

    /**
     * 管理员添加用户（使用默认密码）
     *
     * @param user 待添加的用户
     * @return 持久化后的用户 id
     */
    @Override
    public long addUser(User user) {
        user.setUserPassword(passwordService.encode(DEFAULT_PASSWORD));
        userRepository.save(user);
        return user.getId();
    }

    /**
     * 根据 id 获取用户
     *
     * @param id 用户 id
     * @return 用户信息
     */
    @Override
    public User getUserById(long id) {
        User user = userRepository.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return user;
    }

    /**
     * 获取登录响应对象（脱敏）
     *
     * @param user 用户信息
     * @return 登录响应对象
     */
    public UserLoginVO getUserLoginVO(User user) {
        if (user == null) {
            return null;
        }
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtil.copyProperties(user, userLoginVO);
        return userLoginVO;
    }

    /**
     * 旧版 MD5 密码在登录成功后升级为 BCrypt
     *
     * @param user        登录成功的用户
     * @param rawPassword 明文密码（仅用于重新加密，不落库）
     */
    private void upgradeLegacyPasswordIfNeeded(User user, String rawPassword) {
        if (!passwordService.needsUpgrade(user.getUserPassword())) {
            return;
        }
        try {
            User update = new User();
            update.setId(user.getId());
            update.setUserPassword(passwordService.encode(rawPassword));
            boolean updated = this.updateById(update);
            if (updated) {
                log.info("用户 {} 的密码哈希已升级为 BCrypt", user.getId());
            }
        } catch (Exception e) {
            // 升级失败不影响本次登录，下次登录会再次尝试
            log.warn("密码哈希升级失败，userId={}: {}", user.getId(), e.getMessage());
        }
    }

    /**
     * 从登录态中解析用户 id，兼容 Long / Integer / String 以及旧版 User 对象
     *
     * @param state Session 中保存的登录态
     * @return 用户 id，无法解析时返回 null
     */
    private Long resolveUserId(Object state) {
        if (state == null) {
            return null;
        }
        if (state instanceof Long userId) {
            return userId;
        }
        if (state instanceof Number number) {
            return number.longValue();
        }
        if (state instanceof String text) {
            return StrUtil.isBlank(text) ? null : Long.valueOf(text);
        }
        // 兼容旧版本 Session 中保存的 User 对象（服务重启后通常不会再出现）
        if (state instanceof User user) {
            return user.getId();
        }
        log.warn("无法识别的登录态类型: {}", state.getClass().getName());
        return null;
    }
}
