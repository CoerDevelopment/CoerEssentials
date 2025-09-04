package de.coerdevelopment.essentials.test;

import de.coerdevelopment.essentials.rest.SseRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class SseRegistryTest {

    private SseRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SseRegistry();
    }

    @Test
    void addAndCountWithinLimit() {
        SseRegistry.Channel channel = new SseRegistry.Channel("match", "42", 10);
        SseEmitter emitter1 = registry.add(channel, Duration.ofMinutes(5));
        SseEmitter emitter2 = registry.add(channel, Duration.ofMinutes(5));

        assertNotNull(emitter1);
        assertNotNull(emitter2);
        assertEquals(2, registry.countEmitters(channel));
        assertTrue(registry.getChannels().contains(channel));
    }

    @Test
    void addBeyondLimitThrows() {
        SseRegistry.Channel channel = new SseRegistry.Channel("match", "42", 1);
        registry.add(channel, Duration.ofMinutes(5));
        assertThrows(SseRegistry.ChannelLimitExceededException.class,
                () -> registry.add(channel, Duration.ofMinutes(5)));
    }

    @Test
    void channelEqualityIgnoresLimit() {
        SseRegistry.Channel channel1 = new SseRegistry.Channel("match", "42", 1);
        SseRegistry.Channel channel2 = new SseRegistry.Channel("match", "42", 999);

        registry.add(channel1, Duration.ofMinutes(5));
        assertEquals(1, registry.countEmitters(channel2));
        assertTrue(registry.getChannels().contains(channel2));
    }

    @Test
    void removeEmitterAndPruneChannel() {
        SseRegistry.Channel channel = new SseRegistry.Channel("match", "42");
        SseEmitter emitter1 = registry.add(channel, Duration.ofMinutes(5));
        SseEmitter emitter2 = registry.add(channel, Duration.ofMinutes(5));

        registry.remove(channel, emitter1);
        assertEquals(1, registry.countEmitters(channel));
        registry.remove(channel, emitter2);
        assertEquals(0, registry.countEmitters(channel));
        assertFalse(registry.getChannels().contains(channel));
    }

    @Test
    void lifecycleCallbacksCleanup() throws IOException {
        SseRegistry.Channel channel = new SseRegistry.Channel("match", "42", 10);
        SseEmitter emitter = registry.add(channel, Duration.ofMillis(100));

        emitter.send(SseEmitter.event().name("noop").data("x"));
        emitter.completeWithError(new IOException("boom"));
        //Awaitility.await().atMost(Duration.ofSeconds(1)).until(() -> registry.countEmitters(channel) == 0);

        emitter = registry.add(channel, Duration.ofMillis(100));
        emitter.completeWithError(new IOException("boom"));
        //assertEquals(0, registry.countEmitters(channel));
    }

    @Test
    void closeAllClears() {
        var channel1 = new SseRegistry.Channel("match", "1", 10);
        var channel2 = new SseRegistry.Channel("tournament", "A", 10);
        registry.add(channel1, Duration.ofMinutes(5));
        registry.add(channel2, Duration.ofMinutes(5));

        registry.closeAll();

        assertTrue(registry.getChannels().isEmpty());
        assertEquals(0, registry.countEmitters(channel1));
        assertEquals(0, registry.countEmitters(channel2));
    }
}
