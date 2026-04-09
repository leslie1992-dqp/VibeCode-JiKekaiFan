package com.food.delivery.entity.delivery;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("rider_shift")
public class RiderShiftEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("rider_id")
    private Long riderId;
    private Integer status;
    @TableField("current_task_count")
    private Integer currentTaskCount;
    @TableField("online_at")
    private LocalDateTime onlineAt;
    @TableField("offline_at")
    private LocalDateTime offlineAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRiderId() { return riderId; }
    public void setRiderId(Long riderId) { this.riderId = riderId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getCurrentTaskCount() { return currentTaskCount; }
    public void setCurrentTaskCount(Integer currentTaskCount) { this.currentTaskCount = currentTaskCount; }
    public LocalDateTime getOnlineAt() { return onlineAt; }
    public void setOnlineAt(LocalDateTime onlineAt) { this.onlineAt = onlineAt; }
    public LocalDateTime getOfflineAt() { return offlineAt; }
    public void setOfflineAt(LocalDateTime offlineAt) { this.offlineAt = offlineAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
