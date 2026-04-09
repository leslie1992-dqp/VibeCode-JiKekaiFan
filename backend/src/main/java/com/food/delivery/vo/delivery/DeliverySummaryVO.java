package com.food.delivery.vo.delivery;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class DeliverySummaryVO {
    private Long taskId;
    private Long orderId;
    private Integer status;
    private String statusText;
    private Long riderId;
    private String riderName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedArriveAt;
    /** 下单商家相对用户的演示距离(km)，与首页/商家表一致 */
    private Double routeDistanceKm;
    private DeliveryTrackPointVO latestLocation;
    private List<String> events = Collections.emptyList();

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public Long getRiderId() { return riderId; }
    public void setRiderId(Long riderId) { this.riderId = riderId; }
    public String getRiderName() { return riderName; }
    public void setRiderName(String riderName) { this.riderName = riderName; }
    public LocalDateTime getExpectedArriveAt() { return expectedArriveAt; }
    public void setExpectedArriveAt(LocalDateTime expectedArriveAt) { this.expectedArriveAt = expectedArriveAt; }
    public Double getRouteDistanceKm() { return routeDistanceKm; }
    public void setRouteDistanceKm(Double routeDistanceKm) { this.routeDistanceKm = routeDistanceKm; }
    public DeliveryTrackPointVO getLatestLocation() { return latestLocation; }
    public void setLatestLocation(DeliveryTrackPointVO latestLocation) { this.latestLocation = latestLocation; }
    public List<String> getEvents() { return events; }
    public void setEvents(List<String> events) { this.events = events; }
}
