package com.food.delivery.service.user;

import com.food.delivery.vo.user.UserInfoVO;

public interface UserProfileService {

    UserInfoVO getMe(Long userId);

    /** 更新头像 URL 并返回最新资料 */
    UserInfoVO updateAvatarUrl(Long userId, String avatarUrl);
}
