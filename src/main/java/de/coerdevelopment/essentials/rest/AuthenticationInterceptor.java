package de.coerdevelopment.essentials.rest;

import de.coerdevelopment.essentials.security.CoerSecurity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.http.HttpStatus;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        if (AnnotationUtils.findAnnotation(method.getMethod(), AuthentificationRequired.class) == null &&
                AnnotationUtils.findAnnotation(method.getBeanType(), AuthentificationRequired.class) == null) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired token");
            return false;
        }

        try {
            int accountId = CoerSecurity.getInstance().getIntFromToken(token);
            if (accountId <= 0) {
                throw new Exception();
            }
            request.setAttribute("accountId", accountId);
            return true;
        } catch (Exception e) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired token");
            return false;
        }
    }
}
