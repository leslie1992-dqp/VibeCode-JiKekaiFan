package com.food.delivery.vo.cart;

import com.food.delivery.vo.draft.DraftMerchantVO;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class CartCheckoutPreviewVO {

    private List<DraftMerchantVO> merchants = Collections.emptyList();
    /** 各商家应付之和 */
    private BigDecimal grandPayable;

    public List<DraftMerchantVO> getMerchants() {
        return merchants;
    }

    public void setMerchants(List<DraftMerchantVO> merchants) {
        this.merchants = merchants;
    }

    public BigDecimal getGrandPayable() {
        return grandPayable;
    }

    public void setGrandPayable(BigDecimal grandPayable) {
        this.grandPayable = grandPayable;
    }
}
