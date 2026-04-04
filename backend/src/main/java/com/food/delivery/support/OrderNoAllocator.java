package com.food.delivery.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.entity.order.OrderEntity;
import com.food.delivery.mapper.order.OrderMapper;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class OrderNoAllocator {

    /** 12 位数字：100000000000 ~ 999999999999 */
    private static final long MIN_INCLUSIVE = 100_000_000_000L;
    private static final long MAX_EXCLUSIVE = 1_000_000_000_000L;

    private final OrderMapper orderMapper;

    public OrderNoAllocator(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public String allocate() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 32; i++) {
            String no = String.valueOf(r.nextLong(MIN_INCLUSIVE, MAX_EXCLUSIVE));
            Long cnt = orderMapper.selectCount(
                    new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOrderNo, no)
            );
            if (cnt == null || cnt == 0) {
                return no;
            }
        }
        throw new BizException(50001, "generate order no failed");
    }
}
