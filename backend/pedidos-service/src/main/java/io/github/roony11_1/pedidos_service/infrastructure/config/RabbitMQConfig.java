package io.github.roony11_1.pedidos_service.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig 
{
    public static final String EXCHANGE = "pedidos360.fanout.exchange";
    public static final String NOTIFICACIONES = "pedidos360.notificaciones.queue";
    public static final String AUDITORIA = "pedidos360.auditoria.queue";
    public static final String METRICAS = "pedidos360.metricas.queue";

    @Bean
    FanoutExchange pedidosFanoutExchange() 
    {
        return new FanoutExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue notificacionesQueue() { return new Queue(NOTIFICACIONES, true); }

    @Bean
    Queue auditoriaQueue() { return new Queue(AUDITORIA, true); }

    @Bean
    Queue metricasQueue() { return new Queue(METRICAS, true); }

    @Bean
    Binding notificacionesBinding() {
        return BindingBuilder.bind(notificacionesQueue()).to(pedidosFanoutExchange());
    }

    @Bean
    Binding auditoriaBinding() {
        return BindingBuilder.bind(auditoriaQueue()).to(pedidosFanoutExchange());
    }

    @Bean
    Binding metricasBinding() {
        return BindingBuilder.bind(metricasQueue()).to(pedidosFanoutExchange());
    }
}
