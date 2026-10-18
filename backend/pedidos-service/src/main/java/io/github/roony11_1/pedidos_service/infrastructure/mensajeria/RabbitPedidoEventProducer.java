package io.github.roony11_1.pedidos_service.infrastructure.mensajeria;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import io.github.roony11_1.pedidos_service.core.application.event.PedidoCreadoEvent;
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
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TOPIC,
            RabbitMQConfig.RK_PEDIDO_CREAR,
            event,
            message ->
            {
                var props = message.getMessageProperties();
                props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                props.setMessageId(event.jobId().toString());
                props.setCorrelationId(event.correlationId());
                props.setHeader("idempotencyKey", event.idempotencyKey());
                props.setHeader("userId", event.userId());
                return message;
            });
    }

    @Override
    public void publicarStockReservado(PedidoStockReservadoEvent event)
    {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TOPIC,
            RabbitMQConfig.RK_STOCK_RESERVADO,
            event,
            message ->
            {
                var props = message.getMessageProperties();
                props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                props.setMessageId(event.jobId().toString());
                props.setCorrelationId(event.correlationId());
                props.setHeader("userId", event.userId());
                return message;
            });
    }

    @Override
    public void publicarStockFallido(PedidoStockFallidoEvent event)
    {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TOPIC,
            RabbitMQConfig.RK_STOCK_FALLIDO,
            event,
            message ->
            {
                var props = message.getMessageProperties();
                props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                props.setMessageId(event.jobId().toString());
                props.setCorrelationId(event.correlationId());
                props.setHeader("userId", event.userId());
                return message;
            });
    }

    @Override
    public void publicarEstadoCancelado(PedidoEstadoCanceladoEvent event)
    {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TOPIC,
            RabbitMQConfig.RK_ESTADO_CANCELADO,
            event,
            message ->
            {
                var props = message.getMessageProperties();
                props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                props.setMessageId(event.pedidoId().toString());
                props.setCorrelationId(event.correlationId());
                props.setHeader("userId", event.userId());
                return message;
            });
    }

    @Override
    public void publicarCreadoFinal() 
    {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TOPIC,
            RabbitMQConfig.RK_PEDIDO_CREADO,
            ""
        );
    }
}
