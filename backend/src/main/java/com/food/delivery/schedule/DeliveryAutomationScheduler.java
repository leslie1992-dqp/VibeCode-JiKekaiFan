package com.food.delivery.schedule;

import com.food.delivery.service.delivery.DeliveryAutomationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeliveryAutomationScheduler {

    private final DeliveryAutomationService deliveryAutomationService;

    public DeliveryAutomationScheduler(DeliveryAutomationService deliveryAutomationService) {
        this.deliveryAutomationService = deliveryAutomationService;
    }

    /** 演示配送自动化；间隔较短以便与缩短后的 ETA 对齐 */
    @Scheduled(fixedRate = 5_000)
    public void runAdvanceDemoDelivery() {
        deliveryAutomationService.advanceDemoDeliveryTasks();
    }
}
