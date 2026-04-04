package com.food.delivery.vo.cart;

import com.food.delivery.vo.draft.CheckoutResultVO;

import java.util.Collections;
import java.util.List;

public class CartCheckoutBatchResultVO {

    private List<CheckoutResultVO> orders = Collections.emptyList();

    public List<CheckoutResultVO> getOrders() {
        return orders;
    }

    public void setOrders(List<CheckoutResultVO> orders) {
        this.orders = orders;
    }
}
