package io.github.roony11_1.producto_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.github.roony11_1.producto_service.config.KafkaTopicConfig;
import io.github.roony11_1.producto_service.dto.ProductoAuditoriaEvent;
import io.github.roony11_1.producto_service.dto.ProductoStockEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAfterCommitListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void procesarAuditoria(ProductoAuditoriaEvent evento) {

        String key = evento.productoId().toString();

        kafkaTemplate.send(
            KafkaTopicConfig.TOPIC_AUDITORIA,
            key,
            evento
        );

        log.info(
            "Evento de auditoría enviado a Kafka. eventoId={} productoId={} accion={}",
            evento.eventoId(),
            evento.productoId(),
            evento.accion()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void procesarStock(ProductoStockEvent evento) {

        String key = evento.productoId().toString();

        kafkaTemplate.send(
            KafkaTopicConfig.TOPIC_AUDITORIA,
            key,
            evento
        );

        kafkaTemplate.send(
            KafkaTopicConfig.TOPIC_REPORTERIA,
            key,
            evento
        );

        log.info(
            "Evento de stock enviado a Kafka. eventoId={} productoId={} estado={}",
            evento.eventoId(),
            evento.productoId(),
            evento.estadoReserva()
        );
    }
}