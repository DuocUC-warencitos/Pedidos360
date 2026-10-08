package neytan.rabbitmq_admin_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateQueueRequest(
    @NotBlank (message = "El nombre de la cola es obligatario rey")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]{1,100}$",
        message = "El nombre tiene caracters invalidos"
    )
    String name,
    Boolean durable,
    Boolean exclusive,
    Boolean autoDelete

) 
{}
