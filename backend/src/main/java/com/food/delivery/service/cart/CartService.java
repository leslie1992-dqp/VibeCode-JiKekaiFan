package com.food.delivery.service.cart;

import com.food.delivery.dto.cart.AddCartItemDTO;
import com.food.delivery.vo.cart.CartItemVO;

import java.util.List;

public interface CartService {
    void addItem(Long userId, AddCartItemDTO dto);

    List<CartItemVO> listItems(Long userId);

    void removeItem(Long userId, Long productId);

    void increaseItem(Long userId, Long productId, Integer quantity);

    void decreaseItem(Long userId, Long productId, Integer quantity);

    /** 清空当前用户购物车（有效行置为已删除） */
    void clearAll(Long userId);
}

