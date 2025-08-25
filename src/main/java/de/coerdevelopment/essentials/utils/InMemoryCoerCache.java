package de.coerdevelopment.essentials.utils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class InMemoryCoerCache<K, V> {

    private static class CacheEntry<V> {
        final V value;
        final long expireAt;

        CacheEntry(V value, long ttlSeconds) {
            this.value = value;
            this.expireAt = System.currentTimeMillis() + ttlSeconds * 1000;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    private final long CLEANUP_INTERVAL_SECONDS = 60;
    private final Duration defaultTtl;
    private final ConcurrentHashMap<K, CacheEntry<V>> store = new ConcurrentHashMap<>();

    public InMemoryCoerCache(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                this::cleanup,
                CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS
        );
    }

    public void put(K key, V value) {
        put(key, value, this.defaultTtl.getSeconds());
    }

    public void put(K key, V value, long ttlSeconds) {
        store.put(key, new CacheEntry<>(value, ttlSeconds));
    }

    public V get(K key) {
        CacheEntry<V> entry = store.get(key);
        if (entry == null || entry.isExpired()) {
            remove(key);
            return null;
        }
        return entry.value;
    }

    public void remove(K key) {
        store.remove(key);
    }

    public void cleanup() {
        for (Map.Entry<K, CacheEntry<V>> entry : store.entrySet()) {
            if (entry.getValue().isExpired()) {
                remove(entry.getKey());
            }
        }
    }
}
