package com.food.delivery.impl.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.order.OrderStatus;
import com.food.delivery.entity.merchant.MerchantEntity;
import com.food.delivery.entity.order.OrderEntity;
import com.food.delivery.entity.order.OrderItemEntity;
import com.food.delivery.mapper.merchant.MerchantMapper;
import com.food.delivery.mapper.order.OrderItemMapper;
import com.food.delivery.mapper.order.OrderMapper;
import com.food.delivery.service.order.OrderQueryService;
import com.food.delivery.vo.order.OrderLineItemVO;
import com.food.delivery.vo.order.OrderListItemVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final MerchantMapper merchantMapper;

    public OrderQueryServiceImpl(
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            MerchantMapper merchantMapper
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.merchantMapper = merchantMapper;
    }

    @Override
    public List<OrderListItemVO> listOrders(Long userId) {
        if (userId == null || userId < 1) {
            return Collections.emptyList();
        }
        List<OrderEntity> rows = orderMapper.selectList(
                new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getUserId, userId)
                        .orderByDesc(OrderEntity::getCreatedAt)
        );
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> merchantIds = rows.stream()
                .map(OrderEntity::getMerchantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, MerchantEntity> merchantMap = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            merchantMap.putAll(
                    merchantMapper.selectBatchIds(merchantIds).stream()
                            .collect(Collectors.toMap(MerchantEntity::getId, m -> m, (a, b) -> a))
            );
        }

        List<Long> orderIds = rows.stream().map(OrderEntity::getId).collect(Collectors.toList());
        Map<Long, List<OrderItemEntity>> itemsByOrderId = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<OrderItemEntity> allLines = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItemEntity>()
                            .in(OrderItemEntity::getOrderId, orderIds)
                            .orderByAsc(OrderItemEntity::getId)
            );
            if (allLines != null) {
                for (OrderItemEntity line : allLines) {
                    if (line == null || line.getOrderId() == null) {
                        continue;
                    }
                    itemsByOrderId.computeIfAbsent(line.getOrderId(), k -> new ArrayList<>()).add(line);
                }
            }
        }

        return rows.stream().map(e -> toVo(e, merchantMap, itemsByOrderId)).collect(Collectors.toList());
    }

    private OrderListItemVO toVo(
            OrderEntity e,
            Map<Long, MerchantEntity> merchantMap,
            Map<Long, List<OrderItemEntity>> itemsByOrderId
    ) {
        OrderListItemVO vo = new OrderListItemVO();
        vo.setId(e.getId());
        vo.setOrderNo(e.getOrderNo());
        vo.setMerchantId(e.getMerchantId());
        MerchantEntity m = e.getMerchantId() == null ? null : merchantMap.get(e.getMerchantId());
        vo.setMerchantName(m == null ? null : m.getName());
        vo.setTotalAmount(e.getTotalAmount());
        vo.setGoodsAmount(e.getGoodsAmount());
        vo.setDeliveryFee(e.getDeliveryFee());
        vo.setCouponCode(e.getCouponCode());
        vo.setCouponAmount(e.getCouponAmount());
        vo.setPayAmount(e.getPayAmount());
        vo.setCreatedAt(e.getCreatedAt());

        Integer st = e.getStatus();
        vo.setStatus(st);
        if (st != null && st == OrderStatus.PENDING_PAYMENT) {
            vo.setStatusText("待支付");
            vo.setExpireAt(e.getExpireAt());
        } else if (st != null && st == OrderStatus.CANCELLED) {
            vo.setStatusText("订单取消");
            vo.setExpireAt(null);
        } else {
            vo.setStatusText("成功交易");
            vo.setExpireAt(null);
        }

        List<OrderItemEntity> lines = itemsByOrderId.getOrDefault(e.getId(), Collections.emptyList());
        List<OrderLineItemVO> items = lines.stream().map(this::lineToVo).collect(Collectors.toList());
        vo.setItems(items);
        return vo;
    }

    private OrderLineItemVO lineToVo(OrderItemEntity line) {
        OrderLineItemVO v = new OrderLineItemVO();
        v.setProductId(line.getProductId());
        v.setProductName(line.getProductName());
        v.setQuantity(line.getQuantity());
        v.setUnitPrice(line.getUnitPrice());
        v.setSubtotal(line.getSubtotal());
        return v;
    }
}
