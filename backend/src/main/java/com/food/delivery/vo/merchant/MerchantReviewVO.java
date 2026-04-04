package com.food.delivery.vo.merchant;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class MerchantReviewVO {
    private Long id;
    private Long userId;
    /** 展示名：昵称优先，否则脱敏手机号 */
    private String userDisplayName;
    /** 评价所属商家（个人评论列表等场景返回） */
    private Long merchantId;
    private String merchantName;
    private LocalDateTime createdAt;
    private Integer rating;
    private String content;
    private List<String> imageUrls = Collections.emptyList();
    private List<ReviewRecommendProductVO> recommendProducts = Collections.emptyList();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<ReviewRecommendProductVO> getRecommendProducts() {
        return recommendProducts;
    }

    public void setRecommendProducts(List<ReviewRecommendProductVO> recommendProducts) {
        this.recommendProducts = recommendProducts;
    }
}
