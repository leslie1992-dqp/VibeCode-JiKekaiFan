package com.food.delivery.impl.delivery;

import com.food.delivery.service.delivery.DeliveryRealtimeService;
import com.food.delivery.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class DeliveryRealtimeServiceImpl implements DeliveryRealtimeService {

    private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L;
    private final Map<Long, List<SseEmitter>> orderEmitters = new ConcurrentHashMap<>();
    private final AppProperties appProperties;

    public DeliveryRealtimeServiceImpl(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public SseEmitter subscribe(Long userId, Long orderId) {
        if (!appProperties.getDelivery().isRealtimeEnabled()) {
            SseEmitter emitter = new SseEmitter(1L);
            emitter.complete();
            return emitter;
        }
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        orderEmitters.computeIfAbsent(orderId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(orderId, emitter));
        emitter.onTimeout(() -> remove(orderId, emitter));
        emitter.onError(ex -> remove(orderId, emitter));
        return emitter;
    }

    @Override
    public void publishOrderUpdate(Long orderId, Object payload) {
        if (!appProperties.getDelivery().isRealtimeEnabled()) {
            return;
        }
        List<SseEmitter> list = orderEmitters.get(orderId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("delivery").data(payload));
            } catch (IOException e) {
                remove(orderId, emitter);
            }
        }
    }

    private void remove(Long orderId, SseEmitter emitter) {
        List<SseEmitter> list = orderEmitters.get(orderId);
        if (list == null) {
            return;
        }
        list.remove(emitter);
        if (list.isEmpty()) {
            orderEmitters.remove(orderId);
        }
    }
}
