package com.food.delivery.service.delivery;

public interface DeliveryDispatchService {
    void createAndDispatchForPaidOrder(Long orderId);
    boolean retryDispatch(Long taskId);

    /** 任务结束（送达/失败等）时释放骑手并发占用，与派单时 +1 成对 */
    void releaseRiderTaskSlot(Long riderId);
}
