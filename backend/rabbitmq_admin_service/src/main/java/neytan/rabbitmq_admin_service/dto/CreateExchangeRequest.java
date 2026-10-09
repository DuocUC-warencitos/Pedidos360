package neytan.rabbitmq_admin_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateExchangeRequest(
      @NotBlank(message = "El nombre del exchange es obligatorio")
      @Pattern(
        regexp = "^[a-zA-Z0-9._-]{1,100}$",
        message = "El nombre contiene caracteres inválidos"
    )
    String name,

    @NotBlank(message = "El tipo de exchange es obligatorio")
    @Pattern(
        regexp = "(?i)direct|topic|fanout",
        message = "El tipo debe ser direct, topic o fanout"
    )
    String type,

    Boolean durable
) {}
