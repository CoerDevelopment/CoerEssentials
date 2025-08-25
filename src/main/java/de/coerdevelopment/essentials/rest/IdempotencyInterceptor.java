package de.coerdevelopment.essentials.rest;

import de.coerdevelopment.essentials.utils.CoerCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String IDEMPOTENCY_TTL_SECONDS = "Idempotency-TTL-Seconds";
    private static final long TIMEOUT_MILLISECONDS = 10000;

    private final CoerCache<ResponseEntity> cache;
    private final ConcurrentHashMap<String, CompletableFuture<ResponseEntity<?>>> inFlightRequests;


    public IdempotencyInterceptor() {
        this.cache = new CoerCache<ResponseEntity>("idempotencyCache", Duration.ofMinutes(5), ResponseEntity.class);
        this.inFlightRequests = new ConcurrentHashMap<>();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        IdempotentRequest annotation = method.getMethodAnnotation(IdempotentRequest.class);
        if (annotation == null) {
            return true;
        }

        long ttlSeconds = annotation.ttlSeconds();
        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.isBlank()) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Missing IdempotentRequest-Key header.");
            return false;
        }

        ResponseEntity<?> cached = cache.get(key);
        if (cached != null) {
            writeResponse(response, cached);
            return false;
        }

        CompletableFuture<ResponseEntity<?>> future = inFlightRequests.computeIfAbsent(key, k -> new CompletableFuture<>());

        if (!future.isDone()) {
            request.setAttribute(IDEMPOTENCY_KEY_HEADER, key);
            request.setAttribute("idempotency-future", future);
            request.setAttribute(IDEMPOTENCY_TTL_SECONDS, ttlSeconds);
            return true;
        }

        try {
            ResponseEntity<?> result = future.get(TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
            writeResponse(response, result);
        } catch (TimeoutException e) {
            response.sendError(HttpStatus.REQUEST_TIMEOUT.value(), "Idempotent request still in progress.");
        } catch (Exception e) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error retrieving previous result.");
        }

        request.setAttribute(IDEMPOTENCY_KEY_HEADER, key);
        return true;
    }

    public void cacheResponse(String key, ResponseEntity<?> response, long ttlSeconds) {
        cache.put(key, response, Duration.ofSeconds(ttlSeconds));
        CompletableFuture<ResponseEntity<?>> future = inFlightRequests.get(key);
        if (future != null && !future.isDone()) {
            future.complete(response);
        }
        inFlightRequests.remove(key);
    }

    private void writeResponse(HttpServletResponse response, ResponseEntity<?> entity) throws IOException {
        response.setStatus(entity.getStatusCode().value());
        entity.getHeaders().forEach((k, values) -> values.forEach(v -> response.addHeader(k, v)));
        if (entity.getBody() != null) {
            response.getWriter().write(entity.getBody().toString());
        }
    }
}
