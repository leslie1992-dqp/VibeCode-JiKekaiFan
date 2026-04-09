package com.food.delivery.service.delivery;

import com.food.delivery.dto.delivery.RiderLocationUploadDTO;

public interface RiderDeliveryService {
    void acceptTask(Long riderUserId, Long taskId);
    void pickUpTask(Long riderUserId, Long taskId);
    /** 已取餐后出发配送（PICKING_UP → DELIVERING） */
    void startShippingTask(Long riderUserId, Long taskId);
    void deliverTask(Long riderUserId, Long taskId);
    void reportLocation(Long riderUserId, RiderLocationUploadDTO dto);
}
