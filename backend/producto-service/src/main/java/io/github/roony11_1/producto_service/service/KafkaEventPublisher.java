package io.github.roony11_1.producto_service.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import io.github.roony11_1.producto_service.dto.ProductoAuditoriaEvent;
import io.github.roony11_1.producto_service.dto.ProductoStockEvent;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publicarAuditoria(ProductoAuditoriaEvent evento) {
        applicationEventPublisher.publishEvent(evento);
    }

    public void publicarStock(ProductoStockEvent evento) {
        applicationEventPublisher.publishEvent(evento);
    }
}