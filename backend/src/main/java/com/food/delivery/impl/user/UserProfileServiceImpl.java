package com.food.delivery.impl.user;

import com.food.delivery.common.exception.BizException;
import com.food.delivery.entity.user.UserEntity;
import com.food.delivery.mapper.user.UserMapper;
import com.food.delivery.service.user.UserProfileService;
import com.food.delivery.vo.user.UserInfoVO;
import org.springframework.stereotype.Service;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserMapper userMapper;

    public UserProfileServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserInfoVO getMe(Long userId) {
        UserEntity entity = userMapper.selectById(userId);
        if (entity == null) {
            throw new BizException(40404, "user not found");
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setId(entity.getId());
        vo.setMobile(entity.getMobile());
        vo.setNickname(entity.getNickname());
        vo.setAvatarUrl(entity.getAvatarUrl());
        return vo;
    }

    @Override
    public UserInfoVO updateAvatarUrl(Long userId, String avatarUrl) {
        UserEntity entity = userMapper.selectById(userId);
        if (entity == null) {
            throw new BizException(40404, "user not found");
        }
        entity.setAvatarUrl(avatarUrl);
        entity.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.updateById(entity);
        return getMe(userId);
    }
}
