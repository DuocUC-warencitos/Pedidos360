package neytan.rabbitmq_admin_service.dto;

import java.time.Instant;

public record EventoReciente(
        Instant recibidoAt,
        String routingKey,
        String pedidoId,
        String correlationId) {
}
