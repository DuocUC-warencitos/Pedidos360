package neytan.rabbitmq_admin_service.dto;

import java.util.UUID;

public record ProductoRanking(
        UUID productoId,
        long total) {
}
