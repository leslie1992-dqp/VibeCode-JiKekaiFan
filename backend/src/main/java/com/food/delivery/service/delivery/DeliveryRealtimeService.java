package com.food.delivery.service.delivery;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface DeliveryRealtimeService {
    SseEmitter subscribe(Long userId, Long orderId);
    void publishOrderUpdate(Long orderId, Object payload);
}
