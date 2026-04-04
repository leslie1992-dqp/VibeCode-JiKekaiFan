package com.food.delivery.dto.cart;

import jakarta.validation.constraints.Min;

public class UpdateCartItemQuantityDTO {
    @Min(value = 1, message = "quantity must be >= 1")
    private Integer quantity;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

