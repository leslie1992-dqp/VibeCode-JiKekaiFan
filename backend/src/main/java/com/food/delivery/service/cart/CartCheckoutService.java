package com.food.delivery.service.cart;

import com.food.delivery.vo.cart.CartCheckoutBatchResultVO;
import com.food.delivery.vo.cart.CartCheckoutPreviewVO;

import java.util.List;

public interface CartCheckoutService {

    CartCheckoutPreviewVO preview(Long userId, List<Long> productIds);

    CartCheckoutBatchResultVO checkout(Long userId, List<Long> productIds, boolean immediate);
}
