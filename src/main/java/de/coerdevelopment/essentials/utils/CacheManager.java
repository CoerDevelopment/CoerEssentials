package de.coerdevelopment.essentials.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CacheManager {

    /**
     * Time to live in milliseconds
     */
    private long ttl;
    /**
     * Maximum amount of objects to be stored in the cache
     * If the cache is full, a random object will be removed
     * Set to -1 for unlimited cache size
     */
    private final long maxCacheSize;
    private ConcurrentHashMap<String, Object> cache;

    public CacheManager(long ttl) {
        this(ttl, -1);
    }

    public CacheManager(long ttl, long maxCacheSize) {
        this.ttl = ttl;
        cache = new ConcurrentHashMap<>();
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * Get an object from the cache. If the object is not in the cache, create it with the CacheAction.
     */
    public Object getObject(String key, CacheAction action) {
        Object object = cache.get(key);
        if (object == null) {
            object = action.createObject();
            cacheObject(key, object);
        }
        return object;
    }

    /**
     * Put an object into the cache
     */
    public void cacheObject(String key, Object value) {
        cache.put(key, value);
        // check if the cache is full and remove a random object if so
        if (maxCacheSize > 0 && cache.size() > maxCacheSize) {
            String randomKey = cache.keySet().stream().findAny().orElse(null);
            if (randomKey != null) {
                cache.remove(randomKey);
            }
        }
        // Remove the object after the time to live
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                cache.remove(key);
            }
        }, ttl, TimeUnit.MILLISECONDS);
    }

    /**
     * Get an object from the cache
     */
    public Object getObject(String key) {
        return cache.get(key);
    }

    /**
     * Remove an object from the cache
     */
    public void removeCachedObject(String key) {
        cache.remove(key);
    }

    /**
     * Clear the cache
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Get the cache
     */
    public Map<String, Object> getCache() {
        return cache;
    }

    public long getMaxCacheSize() {
        return maxCacheSize;
    }

    public long getTtl() {
        return ttl;
    }
}
