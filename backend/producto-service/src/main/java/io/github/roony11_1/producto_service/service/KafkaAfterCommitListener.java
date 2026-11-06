package io.github.roony11_1.producto_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.github.roony11_1.producto_service.config.KafkaTopicConfig;
import io.github.roony11_1.producto_service.dto.ProductoAuditoriaEvent;
import io.github.roony11_1.producto_service.dto.StockOperacionEvento;
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
            KafkaTopicConfig.TOPIC_PRODUCTOS,
            key,
            evento
        );
        kafkaTemplate.send(
            KafkaTopicConfig.TOPIC_AUDITORIA,
            key,
            evento
        );

        log.info(
            "Evento de producto enviado a Kafka. eventoId={} productoId={} tipo={}",
            evento.eventoId(),
            evento.productoId(),
            evento.tipo()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void procesarStock(StockOperacionEvento evento) {

        String key = evento.pedidoId().toString();

        kafkaTemplate.send(
            KafkaTopicConfig.TOPIC_STOCK,
            key,
            evento
        );

        kafkaTemplate.send(
            KafkaTopicConfig.TOPIC_AUDITORIA,
            key,
            evento
        );

        log.info(
            "Evento de stock enviado a Kafka. eventoId={} pedidoId={} tipo={} items={}",
            evento.eventoId(),
            evento.pedidoId(),
            evento.tipo(),
            evento.items().size()
        );
    }
}
