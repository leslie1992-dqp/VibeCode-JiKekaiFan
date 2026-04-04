package com.food.delivery.controller.user;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.impl.merchant.MerchantReviewImageStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserMerchantReviewImageController {

    private final MerchantReviewImageStorageService merchantReviewImageStorageService;

    public UserMerchantReviewImageController(MerchantReviewImageStorageService merchantReviewImageStorageService) {
        this.merchantReviewImageStorageService = merchantReviewImageStorageService;
    }

    @PostMapping(value = "/merchant-review-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<String>> uploadReviewImages(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest request
    ) throws IOException {
        return Result.success(merchantReviewImageStorageService.saveAndBuildUrls(files, request));
    }
}
