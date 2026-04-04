package com.food.delivery.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.order.OrderStatus;
import com.food.delivery.entity.order.OrderEntity;
import com.food.delivery.entity.order.OrderItemEntity;
import com.food.delivery.mapper.order.OrderItemMapper;
import com.food.delivery.mapper.order.OrderMapper;
import com.food.delivery.service.coupon.UserCouponLifecycleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderExpirationScheduler {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserCouponLifecycleService userCouponLifecycleService;

    public OrderExpirationScheduler(
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            UserCouponLifecycleService userCouponLifecycleService
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.userCouponLifecycleService = userCouponLifecycleService;
    }

    /** 每分钟清理已过期的待支付订单（整单删除） */
    @Scheduled(fixedRate = 60_000)
    public void removeExpiredPendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<OrderEntity> expired = orderMapper.selectList(
                new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getStatus, OrderStatus.PENDING_PAYMENT)
                        .lt(OrderEntity::getExpireAt, now)
        );
        if (expired == null || expired.isEmpty()) {
            return;
        }
        for (OrderEntity o : expired) {
            if (o == null || o.getId() == null) {
                continue;
            }
            userCouponLifecycleService.releaseByOrderId(o.getId());
            orderItemMapper.delete(
                    new LambdaQueryWrapper<OrderItemEntity>()
                            .eq(OrderItemEntity::getOrderId, o.getId())
            );
            orderMapper.deleteById(o.getId());
        }
    }
}
