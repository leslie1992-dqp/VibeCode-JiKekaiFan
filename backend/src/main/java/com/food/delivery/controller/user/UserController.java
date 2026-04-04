package com.food.delivery.controller.user;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.service.user.UserProfileService;
import com.food.delivery.vo.user.UserInfoVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me(@RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId) {
        return Result.success(userProfileService.getMe(userId));
    }
}
