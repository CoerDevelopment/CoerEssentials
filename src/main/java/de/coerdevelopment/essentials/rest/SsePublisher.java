package de.coerdevelopment.essentials.rest;

import de.coerdevelopment.essentials.CoerEssentials;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SsePublisher {

    private final SseRegistry sseRegistry;

    public SsePublisher(SseRegistry sseRegistry) {
        this.sseRegistry = sseRegistry;
    }

    public void publish(SseRegistry.Channel channel, String eventName, Object payload) {
        String eventId = UUID.randomUUID().toString();
        CopyOnWriteArrayList<SseEmitter> emittersToRemove = new CopyOnWriteArrayList<>();
        for (var emitter : sseRegistry.getEmitters(channel)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).id(eventId).data(payload));
            } catch (Exception e) {
                emittersToRemove.add(emitter);
            }
        }
        for (var emitter : emittersToRemove) {
            CoerEssentials.getInstance().logDebug("SSE send failed on channel " + channel.toString() + ", removing emitter (" + emitter.toString() + ")");
            sseRegistry.remove(channel, emitter);
        }
    }

}
