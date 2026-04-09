package com.food.delivery.service.delivery;

/**
 * 演示环境：无人操作骑手 App 时自动推进接单/取餐/送达，避免界面已接近终点却后端仍超时。
 */
public interface DeliveryAutomationService {

    void advanceDemoDeliveryTasks();
}
