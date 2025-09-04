package de.coerdevelopment.essentials.rest;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@EnableScheduling
public class SseHeartbeat {

    private final SseRegistry sseRegistry;

    public SseHeartbeat(SseRegistry sseRegistry) {
        this.sseRegistry = sseRegistry;
    }

    @Scheduled(fixedRate = 15000)
    public void ping() {
        for (SseRegistry.Channel channel : sseRegistry.getChannels()) {
            for (SseEmitter emitter : sseRegistry.getEmitters(channel)) {
                try {
                    emitter.send(": ping\n\n");
                } catch (Exception e) {
                    sseRegistry.remove(channel, emitter);
                }
            }
        }
    }

}
