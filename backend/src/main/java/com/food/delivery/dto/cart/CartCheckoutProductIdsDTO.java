package com.food.delivery.dto.cart;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class CartCheckoutProductIdsDTO {

    @NotEmpty(message = "productIds required")
    private List<Long> productIds;

    public List<Long> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds;
    }
}
