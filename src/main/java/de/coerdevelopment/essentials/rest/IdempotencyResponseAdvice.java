package de.coerdevelopment.essentials.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class IdempotencyResponseAdvice implements ResponseBodyAdvice<Object> {

    private final IdempotencyInterceptor interceptor;

    public IdempotencyResponseAdvice(IdempotencyInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        String key = (String) servletRequest.getAttribute(IdempotencyInterceptor.IDEMPOTENCY_KEY_HEADER);

        if (key != null) {
            ResponseEntity<Object> entity = ResponseEntity
                    .status(servletResponse.getStatus())
                    .headers(response.getHeaders())
                    .body(body);
            Long ttlSeconds = (Long) servletRequest.getAttribute(IdempotencyInterceptor.IDEMPOTENCY_TTL_SECONDS);
            interceptor.cacheResponse(key, entity, ttlSeconds);
        }

        return body;
    }
}
