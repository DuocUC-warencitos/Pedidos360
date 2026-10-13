package neytan.rabbitmq_admin_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBindingRequest(
        @NotBlank(message = "La cola es obligatoria")
        @Size(max = 100)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
        String queueName,

        @NotBlank(message = "El exchange es obligatorio")
        @Size(max = 100)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
        String exchangeName,

        @Size(max = 255)
        String routingKey
) {}
