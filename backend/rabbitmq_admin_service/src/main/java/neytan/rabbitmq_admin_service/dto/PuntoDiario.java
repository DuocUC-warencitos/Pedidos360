package neytan.rabbitmq_admin_service.dto;

import java.time.LocalDate;

public record PuntoDiario(
        LocalDate fecha,
        String routingKey,
        long total) {
}
