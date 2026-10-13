
package neytan.rabbitmq_admin_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateExchangeRequest(
        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Size(max = 100)
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "El nombre contiene caracteres no permitidos")
        String name,

        @NotBlank(message = "El tipo de exchange es obligatorio")
        @Pattern(
                regexp = "(?i)^(direct|topic|fanout)$",
                message = "El tipo debe ser direct, topic o fanout")
        String type,

        Boolean durable
) {}
