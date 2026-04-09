package com.food.delivery.impl.delivery;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.food.delivery.common.delivery.DeliveryTaskStatus;

import com.food.delivery.common.order.OrderStatus;

import com.food.delivery.config.AppProperties;

import com.food.delivery.entity.delivery.DeliveryTaskEntity;

import com.food.delivery.entity.order.OrderEntity;

import com.food.delivery.mapper.delivery.DeliveryTaskMapper;

import com.food.delivery.mapper.order.OrderMapper;

import com.food.delivery.service.delivery.DeliveryAutomationService;

import com.food.delivery.service.delivery.DeliveryRealtimeService;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDateTime;

import java.util.List;



@Service

public class DeliveryAutomationServiceImpl implements DeliveryAutomationService {



    private final DeliveryTaskMapper deliveryTaskMapper;

    private final OrderMapper orderMapper;

    private final DeliveryDispatchServiceImpl deliveryDispatchService;

    private final DeliveryRealtimeService deliveryRealtimeService;

    private final AppProperties appProperties;



    public DeliveryAutomationServiceImpl(

            DeliveryTaskMapper deliveryTaskMapper,

            OrderMapper orderMapper,

            DeliveryDispatchServiceImpl deliveryDispatchService,

            DeliveryRealtimeService deliveryRealtimeService,

            AppProperties appProperties

    ) {

        this.deliveryTaskMapper = deliveryTaskMapper;

        this.orderMapper = orderMapper;

        this.deliveryDispatchService = deliveryDispatchService;

        this.deliveryRealtimeService = deliveryRealtimeService;

        this.appProperties = appProperties;

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void advanceDemoDeliveryTasks() {

        if (!appProperties.getDelivery().isEnabled()) {

            return;

        }

        AppProperties.Delivery d = appProperties.getDelivery();

        double scale = d.getDemoTimeScale();

        LocalDateTime now = LocalDateTime.now();



        List<DeliveryTaskEntity> assigned = deliveryTaskMapper.selectList(

                new LambdaQueryWrapper<DeliveryTaskEntity>()

                        .eq(DeliveryTaskEntity::getStatus, DeliveryTaskStatus.ASSIGNED)

        );

        for (DeliveryTaskEntity task : assigned) {

            LocalDateTime anchor = task.getCreatedAt();

            if (anchor != null && now.isAfter(plusScaled(anchor, d.getDemoAssignedToArriveMinutes(), scale))) {

                arriveMerchantAsSystem(task, now);

            }

        }



        List<DeliveryTaskEntity> arrived = deliveryTaskMapper.selectList(

                new LambdaQueryWrapper<DeliveryTaskEntity>()

                        .eq(DeliveryTaskEntity::getStatus, DeliveryTaskStatus.ARRIVED_AT_MERCHANT)

        );

        for (DeliveryTaskEntity task : arrived) {

            LocalDateTime anchor = task.getAcceptedAt() != null ? task.getAcceptedAt() : task.getUpdatedAt();

            if (anchor != null && now.isAfter(plusScaled(anchor, d.getDemoArriveToPickupMinutes(), scale))) {

                pickFoodAsSystem(task, now);

            }

        }



        List<DeliveryTaskEntity> picked = deliveryTaskMapper.selectList(

                new LambdaQueryWrapper<DeliveryTaskEntity>()

                        .eq(DeliveryTaskEntity::getStatus, DeliveryTaskStatus.PICKING_UP)

        );

        for (DeliveryTaskEntity task : picked) {

            LocalDateTime anchor = task.getPickedUpAt() != null ? task.getPickedUpAt() : task.getUpdatedAt();

            if (anchor != null && now.isAfter(plusScaled(anchor, d.getDemoPickupToShipMinutes(), scale))) {

                shipAsSystem(task, now);

            }

        }



        List<DeliveryTaskEntity> delivering = deliveryTaskMapper.selectList(

                new LambdaQueryWrapper<DeliveryTaskEntity>()

                        .eq(DeliveryTaskEntity::getStatus, DeliveryTaskStatus.DELIVERING)

                        .isNotNull(DeliveryTaskEntity::getExpectedArriveAt)

        );

        for (DeliveryTaskEntity task : delivering) {

            LocalDateTime trigger = task.getExpectedArriveAt().minusSeconds(

                    Math.max(1L, Math.round(d.getDemoDeliverBeforeEtaMinutes() * 60d * scale))

            );

            if (now.isBefore(trigger)) {

                continue;

            }

            deliverAsSystem(task, now);

        }

    }



    private static LocalDateTime plusScaled(LocalDateTime anchor, double minutes, double scale) {

        long seconds = Math.max(1L, Math.round(minutes * 60d * scale));

        return anchor.plusSeconds(seconds);

    }



    private void arriveMerchantAsSystem(DeliveryTaskEntity task, LocalDateTime now) {

        Integer from = task.getStatus();

        task.setStatus(DeliveryTaskStatus.ARRIVED_AT_MERCHANT);

        task.setAcceptedAt(now);

        task.setUpdatedAt(now);

        deliveryTaskMapper.updateById(task);

        updateOrder(task.getOrderId(), OrderStatus.DELIVERY_ARRIVED_AT_MERCHANT, now);

        deliveryDispatchService.addEvent(task, "ARRIVED_MERCHANT", from, task.getStatus(), "SYSTEM", task.getRiderId(), "demo-auto");

        deliveryRealtimeService.publishOrderUpdate(task.getOrderId(), "DELIVERY_ARRIVED_MERCHANT");

    }



    private void pickFoodAsSystem(DeliveryTaskEntity task, LocalDateTime now) {

        Integer from = task.getStatus();

        task.setStatus(DeliveryTaskStatus.PICKING_UP);

        task.setPickedUpAt(now);

        task.setUpdatedAt(now);

        deliveryTaskMapper.updateById(task);

        updateOrder(task.getOrderId(), OrderStatus.DELIVERY_PICKING_UP, now);

        deliveryDispatchService.addEvent(task, "FOOD_PICKED", from, task.getStatus(), "SYSTEM", task.getRiderId(), "demo-auto");

        deliveryRealtimeService.publishOrderUpdate(task.getOrderId(), "DELIVERY_FOOD_PICKED");

    }



    private void shipAsSystem(DeliveryTaskEntity task, LocalDateTime now) {

        Integer from = task.getStatus();

        task.setStatus(DeliveryTaskStatus.DELIVERING);

        task.setUpdatedAt(now);

        deliveryTaskMapper.updateById(task);

        updateOrder(task.getOrderId(), OrderStatus.DELIVERY_IN_TRANSIT, now);

        deliveryDispatchService.addEvent(task, "SHIPPING", from, task.getStatus(), "SYSTEM", task.getRiderId(), "demo-auto");

        deliveryRealtimeService.publishOrderUpdate(task.getOrderId(), "DELIVERY_SHIPPING");

    }



    private void deliverAsSystem(DeliveryTaskEntity task, LocalDateTime now) {

        Integer from = task.getStatus();

        task.setStatus(DeliveryTaskStatus.DELIVERED);

        task.setDeliveredAt(now);

        task.setUpdatedAt(now);

        deliveryTaskMapper.updateById(task);

        updateOrder(task.getOrderId(), OrderStatus.DELIVERY_DONE, now);

        deliveryDispatchService.addEvent(task, "DELIVERED", from, task.getStatus(), "SYSTEM", task.getRiderId(), "demo-auto");

        deliveryDispatchService.releaseRiderTaskSlot(task.getRiderId());

        deliveryRealtimeService.publishOrderUpdate(task.getOrderId(), "DELIVERY_DONE");

    }



    private void updateOrder(Long orderId, Integer status, LocalDateTime now) {

        OrderEntity order = orderMapper.selectById(orderId);

        if (order == null) {

            return;

        }

        order.setStatus(status);

        order.setUpdatedAt(now);

        orderMapper.updateById(order);

    }

}


