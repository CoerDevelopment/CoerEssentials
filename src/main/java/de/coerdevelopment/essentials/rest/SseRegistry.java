package de.coerdevelopment.essentials.rest;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseRegistry {

    private final ConcurrentHashMap<Channel, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(Channel channel, Duration ttl) {
        if (countEmitters(channel) >= channel.emittersLimit) {
            throw new ChannelLimitExceededException(channel);
        }
        SseEmitter emitter = new SseEmitter(ttl.toMillis());

        Runnable remove = () -> remove(channel, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError((e) -> remove.run());

        emitters.computeIfAbsent(channel, c -> new CopyOnWriteArrayList<>()).add(emitter);
        return emitter;
    }

    public void remove(Channel channel, SseEmitter emitter) {
        emitters.get(channel);
        if (emitters.get(channel) != null) {
            emitters.get(channel).remove(emitter);
            if (emitters.get(channel).isEmpty()) {
                emitters.remove(channel);
            }
        }
    }

    public List<SseEmitter> getEmitters(Channel channel) {
        return emitters.getOrDefault(channel, new CopyOnWriteArrayList<>());
    }

    public int countEmitters(Channel channel) {
        return getEmitters(channel).size();
    }

    public Set<Channel> getChannels() {
        return emitters.keySet();
    }

    @PreDestroy
    public void closeAll() {
        emitters.forEach((ch, list) -> list.forEach(SseEmitter::complete));
        emitters.clear();
    }

    public static class Channel {
        public final String type;
        public final String id;
        public final int emittersLimit;

        public Channel(String type, String id, int emittersLimit) {
            this.type = type;
            this.id = id;
            this.emittersLimit = emittersLimit;
        }

        public Channel(String type, Long id, int emittersLimit) {
            this(type, String.valueOf(id), emittersLimit);
        }

        public Channel(String type, String id) {
            this(type, id, Integer.MAX_VALUE);
        }

        public Channel(String type, Long id) {
            this(type, id, Integer.MAX_VALUE);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Channel) {
                Channel other = (Channel) obj;
                return other.type.equals(this.type) && other.id.equals(this.id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id);
        }

        @Override
        public String toString() {
            return type + ":" + id;
        }
    }

    public class ChannelLimitExceededException extends RuntimeException {
        public ChannelLimitExceededException(Channel channel) {
            super("Too many emitters for channel " + channel);
        }
    }

}
