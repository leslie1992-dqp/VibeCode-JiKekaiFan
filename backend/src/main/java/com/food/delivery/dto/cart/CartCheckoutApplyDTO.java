package com.food.delivery.dto.cart;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class CartCheckoutApplyDTO {

    @NotEmpty(message = "productIds required")
    private List<Long> productIds;

    /** true：立即支付；false：待支付（30 分钟内） */
    private boolean immediate = true;

    public List<Long> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds;
    }

    public boolean isImmediate() {
        return immediate;
    }

    public void setImmediate(boolean immediate) {
        this.immediate = immediate;
    }
}
