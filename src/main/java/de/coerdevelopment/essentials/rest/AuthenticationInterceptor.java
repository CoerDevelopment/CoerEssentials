package de.coerdevelopment.essentials.rest;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.api.Account;
import de.coerdevelopment.essentials.security.CoerSecurity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

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
            int accountId = CoerSecurity.getInstance().getSubjectFromTokenAsInt(token);
            if (accountId <= 0) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired token");
                return false;
            }
            Account account = CoerEssentials.getInstance().getAccountModule().getAccount(accountId);
            if (account.isLocked) {
                response.sendError(HttpStatus.LOCKED.value(), "Account is locked");
                return false;
            }
            if (CoerEssentials.getInstance().getAccountModule().isAccountSpamProtected(accountId)) {
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many requests. Please try again later.");
                return false;
            }
            request.setAttribute("accountId", accountId);
            return true;
        } catch (Exception e) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired token");
            return false;
        }
    }
}
