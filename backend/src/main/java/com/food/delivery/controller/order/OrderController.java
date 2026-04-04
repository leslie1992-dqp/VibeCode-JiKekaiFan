package com.food.delivery.controller.order;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.service.order.OrderPaymentService;
import com.food.delivery.service.order.OrderQueryService;
import com.food.delivery.vo.order.OrderListItemVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderQueryService orderQueryService;
    private final OrderPaymentService orderPaymentService;

    public OrderController(OrderQueryService orderQueryService, OrderPaymentService orderPaymentService) {
        this.orderQueryService = orderQueryService;
        this.orderPaymentService = orderPaymentService;
    }

    @GetMapping
    public Result<List<OrderListItemVO>> listOrders(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId
    ) {
        return Result.success(orderQueryService.listOrders(userId));
    }

    @PostMapping("/{orderId}/pay")
    public Result<Void> payPending(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long orderId
    ) {
        orderPaymentService.payPendingOrder(userId, orderId);
        return Result.success(null);
    }

    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelPending(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long orderId
    ) {
        orderPaymentService.cancelPendingOrder(userId, orderId);
        return Result.success(null);
    }
}
