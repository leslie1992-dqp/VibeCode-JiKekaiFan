package com.food.delivery.impl.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.food.delivery.entity.merchant.MerchantEntity;
import com.food.delivery.entity.order.OrderEntity;
import com.food.delivery.entity.order.OrderItemEntity;
import com.food.delivery.entity.product.ProductEntity;
import com.food.delivery.mapper.merchant.MerchantMapper;
import com.food.delivery.mapper.order.OrderItemMapper;
import com.food.delivery.mapper.order.OrderMapper;
import com.food.delivery.mapper.product.ProductMapper;
import com.food.delivery.service.order.OrderSalesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderSalesServiceImpl implements OrderSalesService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;

    public OrderSalesServiceImpl(
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            ProductMapper productMapper,
            MerchantMapper merchantMapper
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.merchantMapper = merchantMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyPaidOrder(Long orderId) {
        if (orderId == null || orderId < 1) {
            return;
        }
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || order.getMerchantId() == null) {
            return;
        }
        Long merchantId = order.getMerchantId();
        List<OrderItemEntity> lines = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemEntity>()
                        .eq(OrderItemEntity::getOrderId, orderId)
        );
        if (lines == null || lines.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int merchantQtySum = 0;
        for (OrderItemEntity line : lines) {
            if (line == null || line.getProductId() == null) {
                continue;
            }
            int q = line.getQuantity() == null ? 0 : line.getQuantity();
            if (q <= 0) {
                continue;
            }
            merchantQtySum += q;
            productMapper.update(
                    null,
                    new LambdaUpdateWrapper<ProductEntity>()
                            .eq(ProductEntity::getId, line.getProductId())
                            .setSql("sales = IFNULL(sales, 0) + " + q)
                            .set(ProductEntity::getUpdatedAt, now)
            );
        }
        if (merchantQtySum > 0) {
            merchantMapper.update(
                    null,
                    new LambdaUpdateWrapper<MerchantEntity>()
                            .eq(MerchantEntity::getId, merchantId)
                            .setSql("monthly_sales = IFNULL(monthly_sales, 0) + " + merchantQtySum)
                            .set(MerchantEntity::getUpdatedAt, now)
            );
        }
    }
}
