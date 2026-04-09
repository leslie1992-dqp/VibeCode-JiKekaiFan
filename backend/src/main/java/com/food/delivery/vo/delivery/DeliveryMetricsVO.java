package com.food.delivery.vo.delivery;

public class DeliveryMetricsVO {
    private Long waitingDispatchCount;
    private Long deliveringCount;
    private Long deliveredTodayCount;
    private Long failedTodayCount;

    public Long getWaitingDispatchCount() { return waitingDispatchCount; }
    public void setWaitingDispatchCount(Long waitingDispatchCount) { this.waitingDispatchCount = waitingDispatchCount; }
    public Long getDeliveringCount() { return deliveringCount; }
    public void setDeliveringCount(Long deliveringCount) { this.deliveringCount = deliveringCount; }
    public Long getDeliveredTodayCount() { return deliveredTodayCount; }
    public void setDeliveredTodayCount(Long deliveredTodayCount) { this.deliveredTodayCount = deliveredTodayCount; }
    public Long getFailedTodayCount() { return failedTodayCount; }
    public void setFailedTodayCount(Long failedTodayCount) { this.failedTodayCount = failedTodayCount; }
}
