package com.food.delivery.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.common.util.JwtUtil;
import com.food.delivery.common.util.PasswordUtil;
import com.food.delivery.config.AppProperties;
import com.food.delivery.dto.user.UserLoginDTO;
import com.food.delivery.dto.user.UserRegisterDTO;
import com.food.delivery.dto.user.UserResetPasswordDTO;
import com.food.delivery.entity.user.UserEntity;
import com.food.delivery.mapper.user.UserMapper;
import com.food.delivery.service.user.UserAuthService;
import com.food.delivery.vo.user.LoginVO;
import com.food.delivery.vo.user.UserInfoVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserAuthServiceImpl implements UserAuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;

    public UserAuthServiceImpl(UserMapper userMapper, JwtUtil jwtUtil, AppProperties appProperties) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.appProperties = appProperties;
    }

    @Override
    public Long register(UserRegisterDTO dto) {
        UserEntity existed = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getMobile, dto.getMobile())
        );
        if (existed != null) {
            throw new BizException(40902, "该手机号已注册");
        }
        UserEntity entity = new UserEntity();
        entity.setMobile(dto.getMobile());
        entity.setPasswordHash(PasswordUtil.hash(dto.getPassword()));
        entity.setNickname(dto.getNickname());
        entity.setStatus(1);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public LoginVO login(UserLoginDTO dto) {
        UserEntity entity = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getMobile, dto.getMobile())
        );
        if (entity == null) {
            throw new BizException(40401, "该用户不存在，请先注册");
        }
        if (!PasswordUtil.match(dto.getPassword(), entity.getPasswordHash())) {
            throw new BizException(40004, "手机号或密码错误");
        }
        if (entity.getStatus() != null && entity.getStatus() == 0) {
            throw new BizException(40005, "账号已禁用");
        }
        String token = jwtUtil.generateToken(entity.getId(), entity.getMobile());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpireAt(
                System.currentTimeMillis()
                        + appProperties.getJwt().getExpireHours() * 60L * 60L * 1000L
        );
        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setId(entity.getId());
        userInfo.setMobile(entity.getMobile());
        userInfo.setNickname(entity.getNickname());
        userInfo.setAvatarUrl(entity.getAvatarUrl());
        vo.setUserInfo(userInfo);
        return vo;
    }

    @Override
    public void resetPassword(UserResetPasswordDTO dto) {
        UserEntity entity = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getMobile, dto.getMobile())
        );
        if (entity == null) {
            throw new BizException(40401, "该用户不存在，请先注册");
        }
        if (entity.getStatus() != null && entity.getStatus() == 0) {
            throw new BizException(40005, "账号已禁用，无法重置密码");
        }
        entity.setPasswordHash(PasswordUtil.hash(dto.getNewPassword()));
        entity.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(entity);
    }
}
