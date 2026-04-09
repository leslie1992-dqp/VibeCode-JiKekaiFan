package com.food.delivery.impl.delivery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.delivery.DeliveryTaskStatus;
import com.food.delivery.common.order.OrderStatus;
import com.food.delivery.config.AppProperties;
import com.food.delivery.entity.delivery.DeliveryEventEntity;
import com.food.delivery.entity.delivery.DeliveryTaskEntity;
import com.food.delivery.entity.delivery.RiderEntity;
import com.food.delivery.entity.delivery.RiderShiftEntity;
import com.food.delivery.entity.merchant.MerchantEntity;
import com.food.delivery.entity.order.OrderEntity;
import com.food.delivery.entity.user.UserEntity;
import com.food.delivery.mapper.delivery.DeliveryEventMapper;
import com.food.delivery.mapper.delivery.DeliveryTaskMapper;
import com.food.delivery.mapper.delivery.RiderMapper;
import com.food.delivery.mapper.delivery.RiderShiftMapper;
import com.food.delivery.mapper.merchant.MerchantMapper;
import com.food.delivery.mapper.order.OrderMapper;
import com.food.delivery.mapper.user.UserMapper;
import com.food.delivery.service.delivery.DeliveryDispatchService;
import com.food.delivery.service.delivery.DeliveryRealtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DeliveryDispatchServiceImpl implements DeliveryDispatchService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryDispatchServiceImpl.class);

    private final DeliveryTaskMapper deliveryTaskMapper;
    private final RiderMapper riderMapper;
    private final RiderShiftMapper riderShiftMapper;
    private final DeliveryEventMapper deliveryEventMapper;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final DeliveryRealtimeService deliveryRealtimeService;
    private final AppProperties appProperties;
    private final UserMapper userMapper;

    public DeliveryDispatchServiceImpl(
            DeliveryTaskMapper deliveryTaskMapper,
            RiderMapper riderMapper,
            RiderShiftMapper riderShiftMapper,
            DeliveryEventMapper deliveryEventMapper,
            OrderMapper orderMapper,
            MerchantMapper merchantMapper,
            DeliveryRealtimeService deliveryRealtimeService,
            AppProperties appProperties,
            UserMapper userMapper
    ) {
        this.deliveryTaskMapper = deliveryTaskMapper;
        this.riderMapper = riderMapper;
        this.riderShiftMapper = riderShiftMapper;
        this.deliveryEventMapper = deliveryEventMapper;
        this.orderMapper = orderMapper;
        this.merchantMapper = merchantMapper;
        this.deliveryRealtimeService = deliveryRealtimeService;
        this.appProperties = appProperties;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAndDispatchForPaidOrder(Long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        if (!appProperties.getDelivery().isEnabled()) {
            return;
        }
        DeliveryTaskEntity existing = deliveryTaskMapper.selectOne(
                new LambdaQueryWrapper<DeliveryTaskEntity>().eq(DeliveryTaskEntity::getOrderId, orderId)
        );
        if (existing != null) {
            return;
        }
        DeliveryTaskEntity task = new DeliveryTaskEntity();
        task.setOrderId(order.getId());
        task.setMerchantId(order.getMerchantId());
        task.setUserId(order.getUserId());
        task.setStatus(DeliveryTaskStatus.WAITING_DISPATCH);
        task.setDispatchAttempt(0);
        task.setAssignToken(UUID.randomUUID().toString().replace("-", ""));
        MerchantEntity merchant = order.getMerchantId() == null ? null : merchantMapper.selectById(order.getMerchantId());
        double totalMin = appProperties.getDelivery().resolveEtaTotalMinutes(
                merchant == null ? null : merchant.getDistanceKm()
        );
        long etaSeconds = Math.max(30L, Math.round(totalMin * 60d));
        task.setExpectedArriveAt(LocalDateTime.now().plusSeconds(etaSeconds));
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        deliveryTaskMapper.insert(task);
        addEvent(task, "CREATED", null, task.getStatus(), "SYSTEM", null, null);
        assignRider(task, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean retryDispatch(Long taskId) {
        DeliveryTaskEntity task = deliveryTaskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        return assignRider(task, true);
    }

    private boolean assignRider(DeliveryTaskEntity task, boolean retry) {
        ensureDemoRiderAndShiftIfEmpty();
        List<RiderShiftEntity> online = riderShiftMapper.selectList(
                new LambdaQueryWrapper<RiderShiftEntity>()
                        .eq(RiderShiftEntity::getStatus, 1)
                        .orderByAsc(RiderShiftEntity::getCurrentTaskCount)
                        .orderByAsc(RiderShiftEntity::getOnlineAt)
        );
        RiderShiftEntity chosen = online.stream()
                .filter(s -> canTakeMore(s.getRiderId(), s.getCurrentTaskCount()))
                .min(Comparator.comparingInt(s -> s.getCurrentTaskCount() == null ? 0 : s.getCurrentTaskCount()))
                .orElse(null);
        if (chosen == null) {
            chosen = ensureOnlineShiftForAvailableRider();
        }
        if (chosen == null) {
            long rc = riderMapper.selectCount(new LambdaQueryWrapper<RiderEntity>());
            log.warn(
                    "delivery NO_RIDER: orderId={} taskId={} retry={} riderRowCount={}",
                    task.getOrderId(),
                    task.getId(),
                    retry,
                    rc
            );
            task.setDispatchAttempt((task.getDispatchAttempt() == null ? 0 : task.getDispatchAttempt()) + 1);
            task.setUpdatedAt(LocalDateTime.now());
            deliveryTaskMapper.updateById(task);
            addEvent(task, "NO_RIDER", task.getStatus(), task.getStatus(), "SYSTEM", null, retry ? "retry" : "first");
            return false;
        }
        Integer from = task.getStatus();
        task.setRiderId(chosen.getRiderId());
        task.setStatus(DeliveryTaskStatus.ASSIGNED);
        task.setDispatchAttempt((task.getDispatchAttempt() == null ? 0 : task.getDispatchAttempt()) + 1);
        task.setUpdatedAt(LocalDateTime.now());
        deliveryTaskMapper.updateById(task);
        chosen.setCurrentTaskCount((chosen.getCurrentTaskCount() == null ? 0 : chosen.getCurrentTaskCount()) + 1);
        riderShiftMapper.updateById(chosen);

        OrderEntity order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            order.setStatus(OrderStatus.DELIVERY_ASSIGNED);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
        }
        addEvent(task, retry ? "REASSIGNED" : "ASSIGNED", from, task.getStatus(), "SYSTEM", chosen.getRiderId(), null);
        deliveryRealtimeService.publishOrderUpdate(task.getOrderId(), "DELIVERY_ASSIGNED");
        return true;
    }

    @Override
    public void releaseRiderTaskSlot(Long riderId) {
        if (riderId == null) {
            return;
        }
        RiderShiftEntity sh = riderShiftMapper.selectOne(
                new LambdaQueryWrapper<RiderShiftEntity>().eq(RiderShiftEntity::getRiderId, riderId)
        );
        if (sh == null) {
            return;
        }
        int cur = sh.getCurrentTaskCount() == null ? 0 : sh.getCurrentTaskCount();
        if (cur <= 0) {
            return;
        }
        sh.setCurrentTaskCount(cur - 1);
        sh.setUpdatedAt(LocalDateTime.now());
        riderShiftMapper.updateById(sh);
    }

    private boolean canTakeMore(Long riderId, Integer currentTaskCount) {
        RiderEntity rider = riderMapper.selectById(riderId);
        if (rider == null || rider.getStatus() == null || rider.getStatus() != 1) {
            return false;
        }
        int current = currentTaskCount == null ? 0 : currentTaskCount;
        int max = rider.getMaxConcurrentTasks() == null ? 1 : rider.getMaxConcurrentTasks();
        return current < max;
    }

    /**
     * 若库中没有任何骑手（迁移种子未命中 user.id≤3 等），从任意一名正常用户自动建演示骑手 + 在线班次，
     * 否则本地下单会永久待派单。
     */
    private void ensureDemoRiderAndShiftIfEmpty() {
        long n = riderMapper.selectCount(new LambdaQueryWrapper<RiderEntity>());
        if (n > 0) {
            return;
        }
        UserEntity u = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getStatus, 1)
                        .orderByAsc(UserEntity::getId)
                        .last("limit 1")
        );
        if (u == null) {
            log.warn("delivery demo rider skipped: no active user in `user` table");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        RiderEntity r = new RiderEntity();
        r.setUserId(u.getId());
        r.setDisplayName("演示骑手");
        r.setPhone(u.getMobile());
        r.setVehicleType("E_BIKE");
        r.setRating(new BigDecimal("4.80"));
        r.setMaxConcurrentTasks(3);
        r.setStatus(1);
        r.setCreatedAt(now);
        r.setUpdatedAt(now);
        try {
            riderMapper.insert(r);
        } catch (DataIntegrityViolationException ex) {
            log.debug("demo rider insert race or duplicate user_id: {}", ex.getMessage());
            return;
        }
        RiderShiftEntity sh = new RiderShiftEntity();
        sh.setRiderId(r.getId());
        sh.setStatus(1);
        sh.setCurrentTaskCount(0);
        sh.setOnlineAt(now);
        sh.setCreatedAt(now);
        sh.setUpdatedAt(now);
        try {
            riderShiftMapper.insert(sh);
        } catch (DataIntegrityViolationException ex) {
            log.debug("demo rider_shift insert race: {}", ex.getMessage());
        }
    }

    /**
     * 演示/本地环境常见：rider 表有数据但班次为下线(0)或未插入班次，导致永远无「在线」班次。
     * 为首个在职且仍可接单的骑手补齐或切回在线班次后再参与派单。
     */
    private RiderShiftEntity ensureOnlineShiftForAvailableRider() {
        List<RiderEntity> riders = riderMapper.selectList(
                new LambdaQueryWrapper<RiderEntity>()
                        .eq(RiderEntity::getStatus, 1)
                        .orderByAsc(RiderEntity::getId)
                        .last("limit 30")
        );
        for (RiderEntity r : riders) {
            RiderShiftEntity sh = riderShiftMapper.selectOne(
                    new LambdaQueryWrapper<RiderShiftEntity>().eq(RiderShiftEntity::getRiderId, r.getId())
            );
            int current = sh == null || sh.getCurrentTaskCount() == null ? 0 : sh.getCurrentTaskCount();
            if (!canTakeMore(r.getId(), current)) {
                continue;
            }
            if (sh == null) {
                RiderShiftEntity row = new RiderShiftEntity();
                row.setRiderId(r.getId());
                row.setStatus(1);
                row.setCurrentTaskCount(0);
                row.setOnlineAt(LocalDateTime.now());
                riderShiftMapper.insert(row);
                return row;
            }
            Integer st = sh.getStatus();
            if (st == null || st == 0) {
                sh.setStatus(1);
                sh.setOnlineAt(LocalDateTime.now());
                riderShiftMapper.updateById(sh);
                return sh;
            }
            if (st == 1) {
                return sh;
            }
            /** 演示：班次标记为忙碌(2) 时仍允许参与派单（与上面 canTakeMore 一致） */
            if (st == 2) {
                sh.setStatus(1);
                sh.setOnlineAt(LocalDateTime.now());
                riderShiftMapper.updateById(sh);
                return sh;
            }
        }
        return null;
    }

    void addEvent(
            DeliveryTaskEntity task,
            String eventType,
            Integer fromStatus,
            Integer toStatus,
            String operatorType,
            Long operatorId,
            String payload
    ) {
        DeliveryEventEntity event = new DeliveryEventEntity();
        event.setTaskId(task.getId());
        event.setOrderId(task.getOrderId());
        event.setEventType(eventType);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setOperatorType(operatorType);
        event.setOperatorId(operatorId);
        event.setEventTime(LocalDateTime.now());
        event.setPayloadJson(payload);
        event.setCreatedAt(LocalDateTime.now());
        deliveryEventMapper.insert(event);
    }
}
