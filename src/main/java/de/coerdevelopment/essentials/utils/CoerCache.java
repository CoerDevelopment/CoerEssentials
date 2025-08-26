package de.coerdevelopment.essentials.utils;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.coerdevelopment.essentials.CoerEssentials;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.KeyValue;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.http.HttpStatusCode;

import javax.money.CurrencyUnit;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

/**
 * A simple cache implementation using Redis as backend
 */
public final class CoerCache<T> {

    private final RedisCommands<String, String> redis;
    private final String prefix;
    private final Duration defaultTtl;
    private Gson gson;
    private final Class<T> clazz;
    private final Type type;

    public CoerCache(String prefix, Duration defaultTtl, Class<T> clazz) {
        this(prefix, defaultTtl, clazz, null);
    }

    public CoerCache(String prefix, Duration defaultTtl, Type type) {
        this(prefix, defaultTtl, null, type);
    }

    private CoerCache(String prefix, Duration defaultTtl, Class<T> clazz, Type type) {
        if (CoerEssentials.getInstance().getRedisModule() == null) {
            throw new IllegalStateException("Redis module is not enabled");
        }
        this.redis = CoerEssentials.getInstance().getRedisModule().getSharedCommands();
        this.prefix = prefix;
        this.defaultTtl = defaultTtl;
        this.gson = Converters.registerAll(new GsonBuilder()
                .registerTypeAdapter(HttpStatusCode.class, new HttpStatusCodeAdapter())
                .registerTypeAdapter(CurrencyUnit.class, new CurrencyUnitAdapter())
        ).create();
        this.clazz = clazz;
        this.type = type;
    }

    public T get(String key) {
        String value = redis.get(namespace(key));
        if (value == null) {
            return null;
        }
        return deserialize(value);
    }

    public T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
        T value = get(key);
        if (value == null) {
            value = loader.get();
            put(key, value, ttl);
        }
        return value;
    }

    public T getOrLoad(String key, Supplier<T> loader) {
        return getOrLoad(key, this.defaultTtl, loader);
    }

    public void put(String key, T value, Duration ttl) {
        String jsonValue = serialize(value);
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            redis.set(namespace(key), jsonValue);
        } else {
            redis.psetex(namespace(key), ttl.toMillis(), jsonValue);
        }
    }

    public void put(Long key, T value, Duration ttl) {
        put(String.valueOf(key), value, ttl);
    }

    public void put(String key, T value) {
        put(key, value, this.defaultTtl);
    }

    public void put(Long key, T value) {
        put(String.valueOf(key), value);
    }

    public boolean contains(String key) {
        return redis.exists(namespace(key)) > 0;
    }

    public Duration ttlDuration(String key) {
        Long seconds = redis.ttl(namespace(key));
        if (seconds == null || seconds < 0) {
            return Duration.ZERO;
        }
        return Duration.ofSeconds(seconds);
    }

    public void setDuration(String key, Duration ttl) {
        redis.expire(namespace(key), ttl.getSeconds());
    }

    public Map<String, T> getMany(Collection<String> keys) {
        if (keys.isEmpty()) {
            return Map.of();
        }
        List<String> namespacedKeys = keys.stream().map(this::namespace).toList();
        List<KeyValue<String, String>> values = redis.mget(namespacedKeys.toArray(String[]::new));
        Map<String, T> result = new HashMap<>();
        List<String> originalKeys = new ArrayList<>(keys);
        for (int i = 0; i < values.size(); i++) {
            KeyValue<String, String> kv = values.get(i);
            if (kv != null && kv.hasValue()) {
                String originalKey = originalKeys.get(i);
                try {
                    T value = deserialize(kv.getValue());
                    result.put(originalKey, value);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deserialize cache value", e);
                }
            }
        }
        return result;
    }

    public void putMany(Map<String, T> values) {
        putMany(values, this.defaultTtl);
    }

    public void putMany(Map<String, T> values, Duration ttl) {
        for (Map.Entry<String, T> entry : values.entrySet()) {
            put(entry.getKey(), entry.getValue(), ttl);
        }
    }

    public void invalidate(String key) {
        redis.del(namespace(key));
    }

    public void invalidateAll() {
        try {
            final String match = prefix + ":*";
            final ScanArgs scan = ScanArgs.Builder.matches(match).limit(10000);

            String cursor = "0";
            List<String> batch = new ArrayList<>(10000);

            do {
                KeyScanCursor<String> page = redis.scan(ScanCursor.of(cursor), scan);
                cursor = page.getCursor();
                for (String k : page.getKeys()) {
                    batch.add(k);
                    if (batch.size() >= 1000) {
                        unlinkBatch(batch);
                        batch.clear();
                    }
                }
            } while (!"0".equals(cursor));

            if (!batch.isEmpty()) {
                unlinkBatch(batch);
            }
        } catch (Exception ignored) {}
    }

    private String namespace(String key) {
        return prefix + ":" + key;
    }

    private void unlinkBatch(List<String> keys) {
        try {
            redis.unlink(keys.toArray(String[]::new));
        } catch (Exception e) {
            redis.del(keys.toArray(String[]::new));
        }
    }

    private String serialize(T value) {
        if (clazz == String.class && value != null) {
            return (String) value; // Strings roh speichern
        }
        return (clazz != null) ? gson.toJson(value, clazz) : gson.toJson(value, type);
    }

    private T deserialize(String raw) {
        if (clazz == String.class) {
            @SuppressWarnings("unchecked") T t = (T) raw;
            return t;
        }
        return (clazz != null) ? gson.fromJson(raw, clazz) : gson.fromJson(raw, type);
    }

}
