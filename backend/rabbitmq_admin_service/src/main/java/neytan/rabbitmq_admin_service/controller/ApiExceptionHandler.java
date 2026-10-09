package neytan.rabbitmq_admin_service.controller;

import neytan.rabbitmq_admin_service.dto.ApiMessage;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice 
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiMessage> validacion(
            MethodArgumentNotValidException ex) {

        String mensaje = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Parámetros inválidos");

        return ResponseEntity.badRequest()
                .body(new ApiMessage(mensaje, null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiMessage> argumentoInvalido(
            IllegalArgumentException ex) {

        return ResponseEntity.badRequest()
                .body(new ApiMessage(ex.getMessage(), null));
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ApiMessage> errorRabbit(
            AmqpException ex) {

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiMessage(
                        "No se pudo administrar el recurso RabbitMQ",
                        null));
    }

}
