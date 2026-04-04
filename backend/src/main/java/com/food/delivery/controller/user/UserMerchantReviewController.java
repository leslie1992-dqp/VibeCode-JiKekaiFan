package com.food.delivery.controller.user;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.dto.merchant.MerchantReviewCreateDTO;
import com.food.delivery.service.merchant.MerchantReviewService;
import com.food.delivery.vo.common.PageResult;
import com.food.delivery.vo.merchant.MerchantReviewVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserMerchantReviewController {

    private final MerchantReviewService merchantReviewService;

    public UserMerchantReviewController(MerchantReviewService merchantReviewService) {
        this.merchantReviewService = merchantReviewService;
    }

    @GetMapping("/reviews")
    public Result<PageResult<MerchantReviewVO>> listMyReviews(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @RequestParam(value = "pageNo", defaultValue = "1") long pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize
    ) {
        return Result.success(merchantReviewService.pageReviewsByUser(userId, pageNo, pageSize));
    }

    @PostMapping("/merchant-reviews")
    public Result<Long> create(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @Valid @RequestBody MerchantReviewCreateDTO dto
    ) {
        return Result.success(merchantReviewService.createReview(userId, dto));
    }
}
