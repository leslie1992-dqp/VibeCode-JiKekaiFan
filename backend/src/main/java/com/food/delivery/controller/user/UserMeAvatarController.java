package com.food.delivery.controller.user;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.impl.user.UserAvatarStorageService;
import com.food.delivery.service.user.UserProfileService;
import com.food.delivery.vo.user.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserMeAvatarController {

    private final UserAvatarStorageService userAvatarStorageService;
    private final UserProfileService userProfileService;

    public UserMeAvatarController(
            UserAvatarStorageService userAvatarStorageService,
            UserProfileService userProfileService
    ) {
        this.userAvatarStorageService = userAvatarStorageService;
        this.userProfileService = userProfileService;
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserInfoVO> uploadAvatar(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) throws IOException {
        String url = userAvatarStorageService.saveAndBuildUrl(file, request);
        return Result.success(userProfileService.updateAvatarUrl(userId, url));
    }
}
