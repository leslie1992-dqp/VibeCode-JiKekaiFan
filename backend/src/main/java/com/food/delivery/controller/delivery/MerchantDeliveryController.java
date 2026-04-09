package com.food.delivery.controller.delivery;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.service.delivery.DeliveryQueryService;
import com.food.delivery.vo.delivery.DeliverySummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/orders")
public class MerchantDeliveryController {

    private final DeliveryQueryService deliveryQueryService;

    public MerchantDeliveryController(DeliveryQueryService deliveryQueryService) {
        this.deliveryQueryService = deliveryQueryService;
    }

    @GetMapping("/{orderId}/delivery")
    public Result<DeliverySummaryVO> getDeliveryForMerchant(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long merchantUserId,
            @PathVariable Long orderId
    ) {
        return Result.success(deliveryQueryService.getByOrderIdForMerchant(merchantUserId, orderId));
    }
}
