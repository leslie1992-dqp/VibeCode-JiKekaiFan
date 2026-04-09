package com.food.delivery.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.delivery.DeliveryTaskStatus;
import com.food.delivery.common.order.OrderStatus;
import com.food.delivery.entity.delivery.DeliveryTaskEntity;
import com.food.delivery.entity.order.OrderEntity;
import com.food.delivery.mapper.delivery.DeliveryTaskMapper;
import com.food.delivery.mapper.order.OrderMapper;
import com.food.delivery.service.delivery.DeliveryDispatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DeliveryDispatchScheduler {

    private final DeliveryTaskMapper deliveryTaskMapper;
    private final DeliveryDispatchService deliveryDispatchService;
    private final OrderMapper orderMapper;

    public DeliveryDispatchScheduler(
            DeliveryTaskMapper deliveryTaskMapper,
            DeliveryDispatchService deliveryDispatchService,
            OrderMapper orderMapper
    ) {
        this.deliveryTaskMapper = deliveryTaskMapper;
        this.deliveryDispatchService = deliveryDispatchService;
        this.orderMapper = orderMapper;
    }

    /** 每 30 秒扫描一次：对「待派单」且上次更新超过 15 秒的任务重试派单（单任务最多约 6 次尝试） */
    @Scheduled(fixedRate = 30_000)
    public void retryWaitingDispatchTask() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(15);
        List<DeliveryTaskEntity> waitingTasks = deliveryTaskMapper.selectList(
                new LambdaQueryWrapper<DeliveryTaskEntity>()
                        .eq(DeliveryTaskEntity::getStatus, DeliveryTaskStatus.WAITING_DISPATCH)
                        .lt(DeliveryTaskEntity::getUpdatedAt, threshold)
                        .lt(DeliveryTaskEntity::getDispatchAttempt, 6)
                        .orderByAsc(DeliveryTaskEntity::getUpdatedAt)
                        .last("limit 30")
        );
        for (DeliveryTaskEntity task : waitingTasks) {
            deliveryDispatchService.retryDispatch(task.getId());
        }
    }

    /** 每分钟扫描超时未送达任务，转为配送超时 */
    @Scheduled(fixedRate = 60_000)
    public void markDeliveryTimeout() {
        LocalDateTime now = LocalDateTime.now();
        List<DeliveryTaskEntity> timedOutTasks = deliveryTaskMapper.selectList(
                new LambdaQueryWrapper<DeliveryTaskEntity>()
                        .in(
                                DeliveryTaskEntity::getStatus,
                                DeliveryTaskStatus.ASSIGNED,
                                DeliveryTaskStatus.ARRIVED_AT_MERCHANT,
                                DeliveryTaskStatus.PICKING_UP,
                                DeliveryTaskStatus.DELIVERING
                        )
                        .isNotNull(DeliveryTaskEntity::getExpectedArriveAt)
                        .lt(DeliveryTaskEntity::getExpectedArriveAt, now)
                        .orderByAsc(DeliveryTaskEntity::getExpectedArriveAt)
                        .last("limit 50")
        );
        for (DeliveryTaskEntity task : timedOutTasks) {
            if (task.getExpectedArriveAt() != null && now.isBefore(task.getExpectedArriveAt().plusMinutes(5))) {
                // 前端展示的是 ETA 区间（中心值±5分钟），超时判定与展示保持一致。
                continue;
            }
            task.setStatus(DeliveryTaskStatus.FAILED);
            task.setUpdatedAt(now);
            deliveryTaskMapper.updateById(task);
            deliveryDispatchService.releaseRiderTaskSlot(task.getRiderId());
            OrderEntity order = orderMapper.selectById(task.getOrderId());
            if (order != null) {
                order.setStatus(OrderStatus.DELIVERY_FAILED);
                order.setUpdatedAt(now);
                orderMapper.updateById(order);
            }
        }
    }
}
