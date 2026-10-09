package neytan.rabbitmq_admin_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateBindingRequest(
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9._-]{1,100}$")
    String queueName,

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9._-]{1,100}$")
    String exchangeName,

    @Pattern(regexp = "^[a-zA-Z0-9._*#-]{0,100}$")
    String routingKey
) 

{}
