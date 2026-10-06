package io.github.roony11_1.pedidos_service.infrastructure.mensajeria;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;


import io.github.roony11_1.pedidos_service.infrastructure.config.RabbitMQConfig;

@Service
public class HolaMundoConsumer 
{
    @RabbitListener(queues = RabbitMQConfig.NOTIFICACIONES)
    public void consumir(String body) throws Exception 
    {
        System.out.println("[EVENTO PEDIDO DEBUG HOLA MUNDO]" + body.toString());
    }
}
