package io.github.roony11_1.pedidos_service.infrastructure.mensajeria;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import io.github.roony11_1.pedidos_service.core.application.event.PedidoCreadoEvent;
import io.github.roony11_1.pedidos_service.core.application.interfaces.ProcesarPedidoJobUseCase;
import io.github.roony11_1.pedidos_service.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitPedidoJobListener
{
    private final ProcesarPedidoJobUseCase useCase;

    @RabbitListener(
        queues = RabbitMQConfig.PEDIDOS,
        containerFactory = "pedidoRabbitListenerContainerFactory")
    public void onMessage(PedidoCreadoEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag)
        throws Exception
    {
        try
        {
            useCase.procesar(event.jobId());
            channel.basicAck(tag, false);
        }
        catch (Exception ex)
        {
            // Transitorio (5xx, timeout, DB): NACK sin requeue -> DLX/DLQ.
            // Negocio ya fue marcado FAILED dentro del use-case y no llega acá.
            log.warn("NACK jobId={} -> DLQ: {}", event.jobId(), ex.getMessage());
            channel.basicNack(tag, false, false);
        }
    }
}
