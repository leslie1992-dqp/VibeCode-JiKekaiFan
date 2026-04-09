package com.food.delivery.impl.delivery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.delivery.DeliveryTaskStatus;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.entity.delivery.DeliveryEventEntity;
import com.food.delivery.entity.delivery.DeliveryLocationEntity;
import com.food.delivery.entity.delivery.DeliveryTaskEntity;
import com.food.delivery.entity.delivery.RiderEntity;
import com.food.delivery.entity.merchant.MerchantEntity;
import com.food.delivery.entity.order.OrderEntity;
import com.food.delivery.mapper.delivery.DeliveryEventMapper;
import com.food.delivery.mapper.delivery.DeliveryLocationMapper;
import com.food.delivery.mapper.delivery.DeliveryTaskMapper;
import com.food.delivery.mapper.delivery.RiderMapper;
import com.food.delivery.mapper.merchant.MerchantMapper;
import com.food.delivery.mapper.order.OrderMapper;
import com.food.delivery.service.delivery.DeliveryQueryService;
import com.food.delivery.vo.delivery.DeliverySummaryVO;
import com.food.delivery.vo.delivery.DeliveryTrackPointVO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeliveryQueryServiceImpl implements DeliveryQueryService {

    private final DeliveryTaskMapper deliveryTaskMapper;
    private final DeliveryLocationMapper deliveryLocationMapper;
    private final DeliveryEventMapper deliveryEventMapper;
    private final RiderMapper riderMapper;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;

    public DeliveryQueryServiceImpl(
            DeliveryTaskMapper deliveryTaskMapper,
            DeliveryLocationMapper deliveryLocationMapper,
            DeliveryEventMapper deliveryEventMapper,
            RiderMapper riderMapper,
            OrderMapper orderMapper,
            MerchantMapper merchantMapper
    ) {
        this.deliveryTaskMapper = deliveryTaskMapper;
        this.deliveryLocationMapper = deliveryLocationMapper;
        this.deliveryEventMapper = deliveryEventMapper;
        this.riderMapper = riderMapper;
        this.orderMapper = orderMapper;
        this.merchantMapper = merchantMapper;
    }

    @Override
    public DeliverySummaryVO getByOrderIdForUser(Long userId, Long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(40406, "order not found");
        }
        return buildSummary(orderId);
    }

    @Override
    public DeliverySummaryVO getByOrderIdForMerchant(Long merchantId, Long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BizException(40406, "order not found");
        }
        return buildSummary(orderId);
    }

    @Override
    public List<DeliveryTrackPointVO> listTrackPoints(Long userId, Long orderId, int limit) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(40406, "order not found");
        }
        DeliveryTaskEntity task = deliveryTaskMapper.selectOne(
                new LambdaQueryWrapper<DeliveryTaskEntity>().eq(DeliveryTaskEntity::getOrderId, orderId)
        );
        if (task == null) {
            throw new BizException(40407, "delivery task not found");
        }
        if (task.getRiderId() == null
                || task.getStatus() == null
                || task.getStatus() < DeliveryTaskStatus.ASSIGNED) {
            return Collections.emptyList();
        }
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<DeliveryLocationEntity> rows = deliveryLocationMapper.selectList(
                new LambdaQueryWrapper<DeliveryLocationEntity>()
                        .eq(DeliveryLocationEntity::getOrderId, orderId)
                        .orderByDesc(DeliveryLocationEntity::getCreatedAt)
                        .last("limit " + safeLimit)
        );
        Collections.reverse(rows);
        return rows.stream().map(this::toPoint).collect(Collectors.toList());
    }

    private DeliverySummaryVO buildSummary(Long orderId) {
        DeliveryTaskEntity task = deliveryTaskMapper.selectOne(
                new LambdaQueryWrapper<DeliveryTaskEntity>().eq(DeliveryTaskEntity::getOrderId, orderId)
        );
        if (task == null) {
            throw new BizException(40407, "delivery task not found");
        }
        DeliverySummaryVO vo = new DeliverySummaryVO();
        vo.setTaskId(task.getId());
        vo.setOrderId(task.getOrderId());
        vo.setStatus(task.getStatus());
        vo.setStatusText(toStatusText(task.getStatus()));
        vo.setRiderId(task.getRiderId());
        vo.setExpectedArriveAt(task.getExpectedArriveAt());
        if (task.getMerchantId() != null) {
            MerchantEntity m = merchantMapper.selectById(task.getMerchantId());
            if (m != null && m.getDistanceKm() != null) {
                vo.setRouteDistanceKm(m.getDistanceKm().doubleValue());
            }
        }
        if (task.getRiderId() != null) {
            RiderEntity rider = riderMapper.selectById(task.getRiderId());
            vo.setRiderName(rider == null ? null : rider.getDisplayName());
        }
        /** 待派单时尚未绑定骑手，不向用户展示轨迹/坐标，避免与「系统分配中」矛盾 */
        if (task.getRiderId() != null
                && task.getStatus() != null
                && task.getStatus() >= DeliveryTaskStatus.ASSIGNED) {
            DeliveryLocationEntity latest = deliveryLocationMapper.selectOne(
                    new LambdaQueryWrapper<DeliveryLocationEntity>()
                            .eq(DeliveryLocationEntity::getOrderId, orderId)
                            .orderByDesc(DeliveryLocationEntity::getCreatedAt)
                            .last("limit 1")
            );
            if (latest != null) {
                vo.setLatestLocation(toPoint(latest));
            }
        }
        List<DeliveryEventEntity> events = deliveryEventMapper.selectList(
                new LambdaQueryWrapper<DeliveryEventEntity>()
                        .eq(DeliveryEventEntity::getOrderId, orderId)
                        .orderByAsc(DeliveryEventEntity::getEventTime)
        );
        vo.setEvents(events.stream().map(DeliveryEventEntity::getEventType).collect(Collectors.toList()));
        return vo;
    }

    private DeliveryTrackPointVO toPoint(DeliveryLocationEntity e) {
        DeliveryTrackPointVO vo = new DeliveryTrackPointVO();
        vo.setLatitude(e.getLatitude());
        vo.setLongitude(e.getLongitude());
        vo.setSpeedKmh(e.getSpeedKmh());
        vo.setHeading(e.getHeading());
        vo.setClientTime(e.getClientTime());
        vo.setCreatedAt(e.getCreatedAt());
        return vo;
    }

    private String toStatusText(Integer status) {
        if (status == null) return "未知";
        if (status == DeliveryTaskStatus.WAITING_DISPATCH) return "待派单";
        if (status == DeliveryTaskStatus.ASSIGNED) return "已派单";
        if (status == DeliveryTaskStatus.ARRIVED_AT_MERCHANT) return "已到店";
        if (status == DeliveryTaskStatus.PICKING_UP) return "已取餐";
        if (status == DeliveryTaskStatus.DELIVERING) return "配送中";
        if (status == DeliveryTaskStatus.DELIVERED) return "已送达";
        if (status == DeliveryTaskStatus.CANCELLED) return "已取消";
        if (status == DeliveryTaskStatus.FAILED) return "配送超时";
        return "未知";
    }
}
