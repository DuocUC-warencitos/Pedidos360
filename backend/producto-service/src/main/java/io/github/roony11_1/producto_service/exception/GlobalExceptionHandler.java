package io.github.roony11_1.producto_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice 
public class GlobalExceptionHandler 
{
    @ExceptionHandler(StockInsuficienteException.class)
    public ProblemDetail handleStock(StockInsuficienteException ex)
    {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimistic(ObjectOptimisticLockingFailureException ex) 
    {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflicto de concurrencia, reintente la operación");
    }
}
