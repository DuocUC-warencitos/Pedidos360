
package neytan.rabbitmq_admin_service.controller;

import neytan.rabbitmq_admin_service.dto.ApiMessage;

import jakarta.validation.ConstraintViolationException;

import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiMessage> manejarEstado(
            ResponseStatusException ex) {

        String mensaje = ex.getReason() == null
                ? "La operación no pudo completarse."
                : ex.getReason();

        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiMessage(mensaje, null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiMessage> manejarValidacion(
            MethodArgumentNotValidException ex) {

        String mensaje = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Los datos enviados no son válidos.");

        return ResponseEntity.badRequest()
                .body(new ApiMessage(mensaje, null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiMessage> manejarConstraint(
            ConstraintViolationException ex) {

        return ResponseEntity.badRequest()
                .body(new ApiMessage(
                        "Los parámetros enviados no son válidos.", null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiMessage> manejarJson(
            HttpMessageNotReadableException ex) {

        return ResponseEntity.badRequest()
                .body(new ApiMessage(
                        "El cuerpo de la petición no tiene un JSON válido.",
                        null));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiMessage> manejarManagementApi(
            RestClientException ex) {

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiMessage(
                        "No fue posible consultar la API de administración "
                                + "de RabbitMQ. Revisa la conexión y permisos.",
                        null));
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ApiMessage> manejarAmqp(
            AmqpException ex) {

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiMessage(
                        "RabbitMQ no pudo completar la operación.", null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiMessage> manejarArgumento(
            IllegalArgumentException ex) {

        return ResponseEntity.badRequest()
                .body(new ApiMessage(ex.getMessage(), null));
    }
}
