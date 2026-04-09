package com.food.delivery.dto.delivery;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RiderLocationUploadDTO {
    private Long taskId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal speedKmh;
    private BigDecimal heading;
    private LocalDateTime clientTime;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public BigDecimal getSpeedKmh() { return speedKmh; }
    public void setSpeedKmh(BigDecimal speedKmh) { this.speedKmh = speedKmh; }
    public BigDecimal getHeading() { return heading; }
    public void setHeading(BigDecimal heading) { this.heading = heading; }
    public LocalDateTime getClientTime() { return clientTime; }
    public void setClientTime(LocalDateTime clientTime) { this.clientTime = clientTime; }
}
