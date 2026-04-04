package com.food.delivery.dto.merchant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class MerchantReviewCreateDTO {

    @NotNull(message = "请选择商家")
    private Long merchantId;

    @NotNull(message = "请打分")
    @Min(value = 1, message = "评分 1～5")
    @Max(value = 5, message = "评分 1～5")
    private Integer rating;

    @NotBlank(message = "请填写评价内容")
    @Size(max = 2000, message = "评价内容过长")
    private String content;

    @Size(max = 6, message = "最多 6 张图片")
    private List<String> imageUrls = new ArrayList<>();

    @Size(max = 8, message = "推荐菜过多")
    private List<Long> recommendProductIds = new ArrayList<>();

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
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

    public List<Long> getRecommendProductIds() {
        return recommendProductIds;
    }

    public void setRecommendProductIds(List<Long> recommendProductIds) {
        this.recommendProductIds = recommendProductIds;
    }
}
