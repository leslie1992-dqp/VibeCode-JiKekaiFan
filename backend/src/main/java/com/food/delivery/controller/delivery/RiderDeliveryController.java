package com.food.delivery.controller.delivery;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.dto.delivery.RiderLocationUploadDTO;
import com.food.delivery.service.delivery.RiderDeliveryService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rider")
public class RiderDeliveryController {

    private final RiderDeliveryService riderDeliveryService;

    public RiderDeliveryController(RiderDeliveryService riderDeliveryService) {
        this.riderDeliveryService = riderDeliveryService;
    }

    @PostMapping("/tasks/{taskId}/accept")
    public Result<Void> accept(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long riderUserId,
            @PathVariable Long taskId
    ) {
        riderDeliveryService.acceptTask(riderUserId, taskId);
        return Result.success(null);
    }

    @PostMapping("/tasks/{taskId}/pickup")
    public Result<Void> pickup(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long riderUserId,
            @PathVariable Long taskId
    ) {
        riderDeliveryService.pickUpTask(riderUserId, taskId);
        return Result.success(null);
    }

    @PostMapping("/tasks/{taskId}/ship")
    public Result<Void> ship(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long riderUserId,
            @PathVariable Long taskId
    ) {
        riderDeliveryService.startShippingTask(riderUserId, taskId);
        return Result.success(null);
    }

    @PostMapping("/tasks/{taskId}/deliver")
    public Result<Void> deliver(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long riderUserId,
            @PathVariable Long taskId
    ) {
        riderDeliveryService.deliverTask(riderUserId, taskId);
        return Result.success(null);
    }

    @PostMapping("/location")
    public Result<Void> location(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long riderUserId,
            @RequestBody RiderLocationUploadDTO dto
    ) {
        riderDeliveryService.reportLocation(riderUserId, dto);
        return Result.success(null);
    }
}
