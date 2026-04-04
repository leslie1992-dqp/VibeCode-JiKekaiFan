package com.food.delivery.service.merchant;

import com.food.delivery.dto.merchant.MerchantReviewCreateDTO;
import com.food.delivery.vo.common.PageResult;
import com.food.delivery.vo.merchant.MerchantReviewVO;

public interface MerchantReviewService {

    PageResult<MerchantReviewVO> pageReviews(Long merchantId, long pageNo, long pageSize);

    /** 当前用户发出的评价（分页） */
    PageResult<MerchantReviewVO> pageReviewsByUser(Long userId, long pageNo, long pageSize);

    Long createReview(Long userId, MerchantReviewCreateDTO dto);
}
