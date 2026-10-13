
package neytan.rabbitmq_admin_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateQueueRequest(
        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Size(max = 100)
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "El nombre contiene caracteres no permitidos")
        String name,

        Boolean durable,
        Boolean exclusive,
        Boolean autoDelete
) {}
