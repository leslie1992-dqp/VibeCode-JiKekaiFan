package com.food.delivery.controller.delivery;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.service.delivery.DeliveryQueryService;
import com.food.delivery.vo.delivery.DeliverySummaryVO;
import com.food.delivery.vo.delivery.DeliveryTrackPointVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderDeliveryController {

    private final DeliveryQueryService deliveryQueryService;

    public OrderDeliveryController(DeliveryQueryService deliveryQueryService) {
        this.deliveryQueryService = deliveryQueryService;
    }

    @GetMapping("/{orderId}/delivery")
    public Result<DeliverySummaryVO> getDelivery(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long orderId
    ) {
        return Result.success(deliveryQueryService.getByOrderIdForUser(userId, orderId));
    }

    @GetMapping("/{orderId}/delivery/track")
    public Result<List<DeliveryTrackPointVO>> getDeliveryTrack(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "60") Integer limit
    ) {
        return Result.success(deliveryQueryService.listTrackPoints(userId, orderId, limit == null ? 60 : limit));
    }
}
