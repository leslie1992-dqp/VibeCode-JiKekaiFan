package com.food.delivery.controller.user;

import com.food.delivery.common.response.Result;
import com.food.delivery.dto.user.UserLoginDTO;
import com.food.delivery.dto.user.UserRegisterDTO;
import com.food.delivery.dto.user.UserResetPasswordDTO;
import com.food.delivery.service.user.UserAuthService;
import com.food.delivery.vo.user.LoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserAuthService userAuthService;

    public AuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping("/register")
    public Result<Map<String, Long>> register(@Valid @RequestBody UserRegisterDTO dto) {
        Long userId = userAuthService.register(dto);
        Map<String, Long> data = new HashMap<>();
        data.put("userId", userId);
        return Result.success(data);
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return Result.success(userAuthService.login(dto));
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody UserResetPasswordDTO dto) {
        userAuthService.resetPassword(dto);
        return Result.success(null);
    }
}
