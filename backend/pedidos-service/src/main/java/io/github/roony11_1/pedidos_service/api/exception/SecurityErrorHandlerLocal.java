package io.github.roony11_1.pedidos_service.api.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.roony11_1.error.core.ErrorHandler;
import io.github.roony11_1.error.core.ErrorResponse;

/**
 * Handler local para pedidos-service que asegura 403 con ErrorResponse
 * sin mover versión de roony-error-spring. Orden HIGHEST para ganar al genérico.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityErrorHandlerLocal {

    private static final Logger log = LoggerFactory.getLogger(SecurityErrorHandlerLocal.class);

    private final HttpServletRequest request;

    public SecurityErrorHandlerLocal(HttpServletRequest request) {
        this.request = request;
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(Exception ex) {
        log.warn("AccessDenied (local pedidos): {}", ex.getMessage());
        ErrorResponse body = buildEnrichedErrorResponse(ex);
        body.setCode("HTTP-403");
        body.setMessage("Acceso denegado: no tienes permisos para este recurso");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    private ErrorResponse buildEnrichedErrorResponse(Throwable throwable) {
        ErrorResponse response = ErrorHandler.toErrorResponse(throwable);
        response.setPath(request.getRequestURI());
        response.setTraceId(MDC.get("traceId"));
        return response;
    }
}
