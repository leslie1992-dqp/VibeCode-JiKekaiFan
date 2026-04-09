package com.food.delivery.controller.delivery;

import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.service.delivery.DeliveryRealtimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/orders")
public class DeliveryRealtimeController {

    private final DeliveryRealtimeService deliveryRealtimeService;

    public DeliveryRealtimeController(DeliveryRealtimeService deliveryRealtimeService) {
        this.deliveryRealtimeService = deliveryRealtimeService;
    }

    @GetMapping("/{orderId}/delivery/subscribe")
    public SseEmitter subscribe(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long orderId
    ) {
        return deliveryRealtimeService.subscribe(userId, orderId);
    }
}
