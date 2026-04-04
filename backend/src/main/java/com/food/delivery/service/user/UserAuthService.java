package com.food.delivery.service.user;

import com.food.delivery.dto.user.UserLoginDTO;
import com.food.delivery.dto.user.UserRegisterDTO;
import com.food.delivery.dto.user.UserResetPasswordDTO;
import com.food.delivery.vo.user.LoginVO;

public interface UserAuthService {
    Long register(UserRegisterDTO dto);

    LoginVO login(UserLoginDTO dto);

    void resetPassword(UserResetPasswordDTO dto);
}
