package io.github.roony11_1.pedidos_service.api.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.roony11_1.error.core.ErrorHandler;
import io.github.roony11_1.error.core.ErrorResponse;
import io.github.roony11_1.pedidos_service.core.application.service.ProductosNoDisponiblesException;

/**
 * Mapea ERR-1002 a 409 con las IDs problemáticas en details[] para que el
 * frontend las quite del carrito. Orden HIGHEST para ganar al genérico.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProductosNoDisponiblesHandlerLocal {

    private static final Logger log = LoggerFactory.getLogger(ProductosNoDisponiblesHandlerLocal.class);

    private final HttpServletRequest request;

    public ProductosNoDisponiblesHandlerLocal(HttpServletRequest request) {
        this.request = request;
    }

    @ExceptionHandler(ProductosNoDisponiblesException.class)
    public ResponseEntity<ErrorResponse> handleNoDisponibles(ProductosNoDisponiblesException ex) {
        log.info("Prevalidación rechazada: {}", ex.getMessage());
        ErrorResponse body = ErrorHandler.toErrorResponse(ex);
        body.setPath(request.getRequestURI());
        body.setTraceId(MDC.get("traceId"));
        body.setDetails(ex.detalles());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
