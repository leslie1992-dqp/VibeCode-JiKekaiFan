package com.food.delivery.controller.delivery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.delivery.DeliveryTaskStatus;
import com.food.delivery.common.response.Result;
import com.food.delivery.entity.delivery.DeliveryTaskEntity;
import com.food.delivery.mapper.delivery.DeliveryTaskMapper;
import com.food.delivery.vo.delivery.DeliveryMetricsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/delivery")
public class DeliveryOpsController {

    private final DeliveryTaskMapper deliveryTaskMapper;

    public DeliveryOpsController(DeliveryTaskMapper deliveryTaskMapper) {
        this.deliveryTaskMapper = deliveryTaskMapper;
    }

    @GetMapping("/metrics")
    public Result<DeliveryMetricsVO> metrics() {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        DeliveryMetricsVO vo = new DeliveryMetricsVO();
        vo.setWaitingDispatchCount(deliveryTaskMapper.selectCount(
                new LambdaQueryWrapper<DeliveryTaskEntity>().eq(DeliveryTaskEntity::getStatus, DeliveryTaskStatus.WAITING_DISPATCH)
        ));
        vo.setDeliveringCount(deliveryTaskMapper.selectCount(
                new LambdaQueryWrapper<DeliveryTaskEntity>()
                        .in(
                                DeliveryTaskEntity::getStatus,
                                DeliveryTaskStatus.ASSIGNED,
                                DeliveryTaskStatus.ARRIVED_AT_MERCHANT,
                                DeliveryTaskStatus.PICKING_UP,
                                DeliveryTaskStatus.DELIVERING
                        )
        ));
        vo.setDeliveredTodayCount(deliveryTaskMapper.selectCount(
                new LambdaQueryWrapper<DeliveryTaskEntity>()
                        .eq(DeliveryTaskEntity::getStatus, DeliveryTaskStatus.DELIVERED)
                        .ge(DeliveryTaskEntity::getUpdatedAt, start)
        ));
        vo.setFailedTodayCount(deliveryTaskMapper.selectCount(
                new LambdaQueryWrapper<DeliveryTaskEntity>()
                        .eq(DeliveryTaskEntity::getStatus, DeliveryTaskStatus.FAILED)
                        .ge(DeliveryTaskEntity::getUpdatedAt, start)
        ));
        return Result.success(vo);
    }
}
