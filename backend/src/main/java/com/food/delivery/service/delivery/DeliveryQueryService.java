package com.food.delivery.service.delivery;

import com.food.delivery.vo.delivery.DeliverySummaryVO;
import com.food.delivery.vo.delivery.DeliveryTrackPointVO;

import java.util.List;

public interface DeliveryQueryService {
    DeliverySummaryVO getByOrderIdForUser(Long userId, Long orderId);
    DeliverySummaryVO getByOrderIdForMerchant(Long merchantId, Long orderId);
    List<DeliveryTrackPointVO> listTrackPoints(Long userId, Long orderId, int limit);
}
