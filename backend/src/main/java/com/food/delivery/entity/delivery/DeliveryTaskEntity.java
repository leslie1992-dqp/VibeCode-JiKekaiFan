package com.food.delivery.entity.delivery;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("delivery_task")
public class DeliveryTaskEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("order_id")
    private Long orderId;
    @TableField("merchant_id")
    private Long merchantId;
    @TableField("user_id")
    private Long userId;
    @TableField("rider_id")
    private Long riderId;
    private Integer status;
    @TableField("dispatch_attempt")
    private Integer dispatchAttempt;
    @TableField("assign_token")
    private String assignToken;
    @TableField("expected_arrive_at")
    private LocalDateTime expectedArriveAt;
    @TableField("accepted_at")
    private LocalDateTime acceptedAt;
    @TableField("picked_up_at")
    private LocalDateTime pickedUpAt;
    @TableField("delivered_at")
    private LocalDateTime deliveredAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRiderId() { return riderId; }
    public void setRiderId(Long riderId) { this.riderId = riderId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getDispatchAttempt() { return dispatchAttempt; }
    public void setDispatchAttempt(Integer dispatchAttempt) { this.dispatchAttempt = dispatchAttempt; }
    public String getAssignToken() { return assignToken; }
    public void setAssignToken(String assignToken) { this.assignToken = assignToken; }
    public LocalDateTime getExpectedArriveAt() { return expectedArriveAt; }
    public void setExpectedArriveAt(LocalDateTime expectedArriveAt) { this.expectedArriveAt = expectedArriveAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(LocalDateTime pickedUpAt) { this.pickedUpAt = pickedUpAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
