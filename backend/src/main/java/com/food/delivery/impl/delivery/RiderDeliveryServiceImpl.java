package com.food.delivery.impl.delivery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.delivery.DeliveryTaskStatus;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.common.order.OrderStatus;
import com.food.delivery.config.AppProperties;
import com.food.delivery.dto.delivery.RiderLocationUploadDTO;
import com.food.delivery.entity.delivery.DeliveryLocationEntity;
import com.food.delivery.entity.delivery.DeliveryTaskEntity;
import com.food.delivery.entity.delivery.RiderEntity;
import com.food.delivery.entity.order.OrderEntity;
import com.food.delivery.mapper.delivery.DeliveryLocationMapper;
import com.food.delivery.mapper.delivery.DeliveryTaskMapper;
import com.food.delivery.mapper.delivery.RiderMapper;
import com.food.delivery.mapper.order.OrderMapper;
import com.food.delivery.service.delivery.DeliveryRealtimeService;
import com.food.delivery.service.delivery.RiderDeliveryService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RiderDeliveryServiceImpl implements RiderDeliveryService {

    private final RiderMapper riderMapper;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final DeliveryLocationMapper deliveryLocationMapper;
    private final OrderMapper orderMapper;
    private final DeliveryDispatchServiceImpl deliveryDispatchService;
    private final DeliveryRealtimeService deliveryRealtimeService;
    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    public RiderDeliveryServiceImpl(
            RiderMapper riderMapper,
            DeliveryTaskMapper deliveryTaskMapper,
            DeliveryLocationMapper deliveryLocationMapper,
            OrderMapper orderMapper,
            DeliveryDispatchServiceImpl deliveryDispatchService,
            DeliveryRealtimeService deliveryRealtimeService,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            AppProperties appProperties
    ) {
        this.riderMapper = riderMapper;
        this.deliveryTaskMapper = deliveryTaskMapper;
        this.deliveryLocationMapper = deliveryLocationMapper;
        this.orderMapper = orderMapper;
        this.deliveryDispatchService = deliveryDispatchService;
        this.deliveryRealtimeService = deliveryRealtimeService;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.appProperties = appProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptTask(Long riderUserId, Long taskId) {
        DeliveryTaskEntity task = requireOwnedTask(riderUserId, taskId);
        if (task.getStatus() == null || task.getStatus() != DeliveryTaskStatus.ASSIGNED) {
            throw new BizException(40021, "task is not assignable");
        }
        Integer from = task.getStatus();
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(DeliveryTaskStatus.ARRIVED_AT_MERCHANT);
        task.setAcceptedAt(now);
        task.setUpdatedAt(now);
        deliveryTaskMapper.updateById(task);
        updateOrderStatus(task.getOrderId(), OrderStatus.DELIVERY_ARRIVED_AT_MERCHANT);
        deliveryDispatchService.addEvent(task, "ARRIVED_MERCHANT", from, task.getStatus(), "RIDER", task.getRiderId(), null);
        deliveryRealtimeService.publishOrderUpdate(task.getOrderId(), "DELIVERY_ARRIVED_MERCHANT");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickUpTask(Long riderUserId, Long taskId) {
        DeliveryTaskEntity task = requireOwnedTask(riderUserId, taskId);
        if (task.getStatus() == null || task.getStatus() != DeliveryTaskStatus.ARRIVED_AT_MERCHANT) {
            throw new BizException(40022, "task is not at merchant");
        }
        Integer from = task.getStatus();
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(DeliveryTaskStatus.PICKING_UP);
        task.setPickedUpAt(now);
        task.setUpdatedAt(now);
        deliveryTaskMapper.updateById(task);
        updateOrderStatus(task.getOrderId(), OrderStatus.DELIVERY_PICKING_UP);
        deliveryDispatchService.addEvent(task, "FOOD_PICKED", from, task.getStatus(), "RIDER", task.getRiderId(), null);
        deliveryRealtimeService.publishOrderUpdate(task.getOrderId(), "DELIVERY_FOOD_PICKED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startShippingTask(Long riderUserId, Long taskId) {
        DeliveryTaskEntity task = requireOwnedTask(riderUserId, taskId);
        if (task.getStatus() == null || task.getStatus() != DeliveryTaskStatus.PICKING_UP) {
            throw new BizException(40024, "task is not ready to ship");
        }
        Integer from = task.getStatus();
        task.setStatus(DeliveryTaskStatus.DELIVERING);
        task.setUpdatedAt(LocalDateTime.now());
        deliveryTaskMapper.updateById(task);
        updateOrderStatus(task.getOrderId(), OrderStatus.DELIVERY_IN_TRANSIT);
        deliveryDispatchService.addEvent(task, "SHIPPING", from, task.getStatus(), "RIDER", task.getRiderId(), null);
        deliveryRealtimeService.publishOrderUpdate(task.getOrderId(), "DELIVERY_SHIPPING");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverTask(Long riderUserId, Long taskId) {
        DeliveryTaskEntity task = requireOwnedTask(riderUserId, taskId);
        if (task.getStatus() == null || task.getStatus() != DeliveryTaskStatus.DELIVERING) {
            throw new BizException(40023, "task is not delivering");
        }
        Integer from = task.getStatus();
        task.setStatus(DeliveryTaskStatus.DELIVERED);
        task.setDeliveredAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        deliveryTaskMapper.updateById(task);
        updateOrderStatus(task.getOrderId(), OrderStatus.DELIVERY_DONE);
        deliveryDispatchService.addEvent(task, "DELIVERED", from, task.getStatus(), "RIDER", task.getRiderId(), null);
        deliveryDispatchService.releaseRiderTaskSlot(task.getRiderId());
        deliveryRealtimeService.publishOrderUpdate(task.getOrderId(), "DELIVERY_DONE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportLocation(Long riderUserId, RiderLocationUploadDTO dto) {
        if (dto == null || dto.getTaskId() == null || dto.getLatitude() == null || dto.getLongitude() == null) {
            throw new BizException(40001, "invalid request");
        }
        DeliveryTaskEntity task = requireOwnedTask(riderUserId, dto.getTaskId());
        validateRateLimit(task.getRiderId());
        DeliveryLocationEntity row = new DeliveryLocationEntity();
        row.setTaskId(task.getId());
        row.setOrderId(task.getOrderId());
        row.setRiderId(task.getRiderId());
        row.setLatitude(dto.getLatitude());
        row.setLongitude(dto.getLongitude());
        row.setSpeedKmh(dto.getSpeedKmh());
        row.setHeading(dto.getHeading());
        row.setClientTime(dto.getClientTime());
        row.setCreatedAt(LocalDateTime.now());
        if (appProperties.getDelivery().isLocationStoreEnabled()) {
            deliveryLocationMapper.insert(row);
        }

        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(
                    "delivery:loc:order:" + task.getOrderId(),
                    dto.getLatitude() + "," + dto.getLongitude() + "," + LocalDateTime.now()
            );
        }
        deliveryRealtimeService.publishOrderUpdate(task.getOrderId(), "LOCATION_UPDATED");
    }

    private DeliveryTaskEntity requireOwnedTask(Long riderUserId, Long taskId) {
        RiderEntity rider = riderMapper.selectOne(
                new LambdaQueryWrapper<RiderEntity>().eq(RiderEntity::getUserId, riderUserId)
        );
        if (rider == null) {
            throw new BizException(40301, "rider role required");
        }
        DeliveryTaskEntity task = deliveryTaskMapper.selectById(taskId);
        if (task == null || task.getRiderId() == null || !task.getRiderId().equals(rider.getId())) {
            throw new BizException(40407, "delivery task not found");
        }
        return task;
    }

    private void updateOrderStatus(Long orderId, Integer status) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private void validateRateLimit(Long riderId) {
        if (redisTemplate == null || riderId == null) {
            return;
        }
        String key = "delivery:rider:last-upload:" + riderId;
        String prev = redisTemplate.opsForValue().get(key);
        long now = System.currentTimeMillis();
        if (prev != null) {
            long last = Long.parseLong(prev);
            if (now - last < appProperties.getDelivery().getLocationMinIntervalMs()) {
                throw new BizException(42901, "location upload too frequent");
            }
        }
        redisTemplate.opsForValue().set(key, String.valueOf(now));
    }
}
