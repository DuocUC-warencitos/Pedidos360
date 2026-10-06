package io.github.roony11_1.pedidos_service.infrastructure.mensajeria;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import io.github.roony11_1.pedidos_service.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class PedidoEventoProducer 
{
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void publicar() 
    {
        try 
        {
            String json = "Hola mundo";

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    "",
                    json,
                    message -> 
                    {
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);

                        return message;
                    });
        } 
        catch (Exception e) 
        {
            throw new IllegalStateException("No fue posible serializar el evento" + e.getMessage(), e);
        }
    }
}