package com.food.delivery.service.draft;

import com.food.delivery.dto.cart.AddCartItemDTO;
import com.food.delivery.vo.draft.CheckoutResultVO;
import com.food.delivery.vo.draft.DraftMerchantVO;

public interface MerchantDraftService {

    void addItem(Long userId, AddCartItemDTO dto);

    DraftMerchantVO getDraft(Long userId, Long merchantId);

    void increaseItem(Long userId, Long merchantId, Long productId, Integer quantity);

    void decreaseItem(Long userId, Long merchantId, Long productId, Integer quantity);

    void removeItem(Long userId, Long merchantId, Long productId);

    CheckoutResultVO checkout(Long userId, Long merchantId);

    /**
     * 取消立即支付：从草稿生成「待支付」订单，30 分钟内需支付，超时自动删除。
     */
    CheckoutResultVO checkoutPending(Long userId, Long merchantId);
}
