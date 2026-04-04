package com.food.delivery.vo.draft;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class DraftMerchantVO {

    private Long merchantId;
    /** 可选：购物车结算预览等场景展示用 */
    private String merchantName;
    private List<DraftItemVO> items = Collections.emptyList();
    /** 该商家待下单商品件数：sum(quantity) */
    private Integer totalQuantity;
    private String couponCode;
    private BigDecimal couponAmount;
    /** none | no_coupon | ineligible | applied */
    private String couponLineStatus;
    /** 满减规则文案 */
    private String couponRuleText;
    /** 无 / 暂不可用 / 已减¥x */
    private String couponHint;
    /** 商品小计（不含配送费） */
    private BigDecimal goodsAmount;
    /** 配送费 */
    private BigDecimal deliveryFee;
    /** 订单合计（商品 + 配送，扣券前） */
    private BigDecimal originalAmount;
    private BigDecimal payableAmount;

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public List<DraftItemVO> getItems() {
        return items;
    }

    public void setItems(List<DraftItemVO> items) {
        this.items = items;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public BigDecimal getCouponAmount() {
        return couponAmount;
    }

    public void setCouponAmount(BigDecimal couponAmount) {
        this.couponAmount = couponAmount;
    }

    public String getCouponLineStatus() {
        return couponLineStatus;
    }

    public void setCouponLineStatus(String couponLineStatus) {
        this.couponLineStatus = couponLineStatus;
    }

    public String getCouponRuleText() {
        return couponRuleText;
    }

    public void setCouponRuleText(String couponRuleText) {
        this.couponRuleText = couponRuleText;
    }

    public String getCouponHint() {
        return couponHint;
    }

    public void setCouponHint(String couponHint) {
        this.couponHint = couponHint;
    }

    public BigDecimal getGoodsAmount() {
        return goodsAmount;
    }

    public void setGoodsAmount(BigDecimal goodsAmount) {
        this.goodsAmount = goodsAmount;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public void setPayableAmount(BigDecimal payableAmount) {
        this.payableAmount = payableAmount;
    }
}
