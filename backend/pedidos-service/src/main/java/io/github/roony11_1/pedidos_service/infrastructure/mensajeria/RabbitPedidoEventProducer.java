package io.github.roony11_1.pedidos_service.infrastructure.mensajeria;

import java.util.Map;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import io.github.roony11_1.pedidos_service.core.application.event.PedidoCreacionCompletadaEvent;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoCreadoEvent;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoEstadoActualizadoEvent;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoEstadoCanceladoEvent;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoStockFallidoEvent;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoStockReservadoEvent;
import io.github.roony11_1.pedidos_service.core.application.interfaces.PedidoEventPublisher;
import io.github.roony11_1.pedidos_service.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RabbitPedidoEventProducer implements PedidoEventPublisher
{
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publicarCreado(PedidoCreadoEvent event)
    {
        publicar(
            RabbitMQConfig.RK_PEDIDO_CREAR,
            event,
            event.jobId().toString(),
            event.correlationId(),
            Map.of("idempotencyKey", event.idempotencyKey(), "userId", event.userId()));
    }

    @Override
    public void publicarCreacionCompletada(PedidoCreacionCompletadaEvent event)
    {
        publicar(
            RabbitMQConfig.RK_PEDIDO_CREADO,
            event,
            event.jobId().toString(),
            event.correlationId(),
            Map.of("userId", event.userId()));
    }

    @Override
    public void publicarStockReservado(PedidoStockReservadoEvent event)
    {
        publicar(
            RabbitMQConfig.RK_STOCK_RESERVADO,
            event,
            event.jobId().toString(),
            event.correlationId(),
            Map.of("userId", event.userId()));
    }

    @Override
    public void publicarStockFallido(PedidoStockFallidoEvent event)
    {
        publicar(
            RabbitMQConfig.RK_STOCK_FALLIDO,
            event,
            event.jobId().toString(),
            event.correlationId(),
            Map.of("userId", event.userId()));
    }

    @Override
    public void publicarEstadoCancelado(PedidoEstadoCanceladoEvent event)
    {
        publicar(
            RabbitMQConfig.RK_ESTADO_CANCELADO,
            event,
            event.pedidoId().toString(),
            event.correlationId(),
            Map.of("userId", event.userId()));
    }

    private void publicar(String routingKey, Object event, String messageId, String correlationId, Map<String, String> headers)
    {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TOPIC,
            routingKey,
            event,
            message ->
            {
                var props = message.getMessageProperties();
                props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

                if (messageId != null)
                    props.setMessageId(messageId);

                if (correlationId != null)
                    props.setCorrelationId(correlationId);

                headers.forEach(props::setHeader);

                return message;
            });
    }

    
    
    @Override
    public void publicarEstadoActualizado(PedidoEstadoActualizadoEvent event)
    {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TOPIC,
            "pedido.estado.actualizado",
            event,
            message -> {
                message.getMessageProperties()
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                message.getMessageProperties()
                    .setMessageId(event.pedidoId() + "-" + event.estado());
                message.getMessageProperties()
                    .setHeader("userId", event.userId());
                return message;
            }
        );
    }


}
