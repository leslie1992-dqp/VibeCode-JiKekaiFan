package com.food.delivery.impl.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.common.order.OrderStatus;
import com.food.delivery.entity.order.OrderEntity;
import com.food.delivery.mapper.order.OrderMapper;
import com.food.delivery.service.delivery.DeliveryDispatchService;
import com.food.delivery.service.coupon.UserCouponLifecycleService;
import com.food.delivery.service.order.OrderPaymentService;
import com.food.delivery.service.order.OrderSalesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private final OrderMapper orderMapper;
    private final UserCouponLifecycleService userCouponLifecycleService;
    private final OrderSalesService orderSalesService;
    private final DeliveryDispatchService deliveryDispatchService;

    public OrderPaymentServiceImpl(
            OrderMapper orderMapper,
            UserCouponLifecycleService userCouponLifecycleService,
            OrderSalesService orderSalesService,
            DeliveryDispatchService deliveryDispatchService
    ) {
        this.orderMapper = orderMapper;
        this.userCouponLifecycleService = userCouponLifecycleService;
        this.orderSalesService = orderSalesService;
        this.deliveryDispatchService = deliveryDispatchService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payPendingOrder(Long userId, Long orderId) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        if (orderId == null || orderId < 1) {
            throw new BizException(40001, "invalid request");
        }

        OrderEntity order = orderMapper.selectOne(
                new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getId, orderId)
                        .eq(OrderEntity::getUserId, userId)
        );
        if (order == null) {
            throw new BizException(40406, "order not found");
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BizException(40009, "order is not pending payment");
        }
        LocalDateTime now = LocalDateTime.now();
        if (order.getExpireAt() != null && order.getExpireAt().isBefore(now)) {
            throw new BizException(40010, "order has expired");
        }

        order.setStatus(OrderStatus.PAID_SUCCESS);
        order.setExpireAt(null);
        order.setUpdatedAt(now);
        orderMapper.updateById(order);
        if (order.getUserCouponId() != null) {
            userCouponLifecycleService.markUsed(order.getUserCouponId());
        }
        orderSalesService.applyPaidOrder(orderId);
        deliveryDispatchService.createAndDispatchForPaidOrder(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPendingOrder(Long userId, Long orderId) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        if (orderId == null || orderId < 1) {
            throw new BizException(40001, "invalid request");
        }

        OrderEntity order = orderMapper.selectOne(
                new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getId, orderId)
                        .eq(OrderEntity::getUserId, userId)
        );
        if (order == null) {
            throw new BizException(40406, "order not found");
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BizException(40009, "order is not pending payment");
        }

        LocalDateTime now = LocalDateTime.now();
        userCouponLifecycleService.releaseByOrderId(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        order.setExpireAt(null);
        order.setUpdatedAt(now);
        orderMapper.updateById(order);
    }
}
