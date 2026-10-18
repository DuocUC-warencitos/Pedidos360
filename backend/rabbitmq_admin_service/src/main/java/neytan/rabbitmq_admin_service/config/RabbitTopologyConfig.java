package neytan.rabbitmq_admin_service.config;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    public static final String TOPIC = "pedidos360.topic";
    public static final String DLX = "pedidos360.dlx";
    public static final String DLQ_ROUTING_KEY = "pedidos.failed";

    public static final String AUDITORIA = "pedidos360.auditoria.queue";
    public static final String METRICAS = "pedidos360.metricas.queue";
    public static final String NOTIFICACIONES = "pedidos360.notificaciones.queue";

    public static final String AUDITORIA_DLQ = AUDITORIA + ".dlq";
    public static final String METRICAS_DLQ = METRICAS + ".dlq";
    public static final String NOTIFICACIONES_DLQ = NOTIFICACIONES + ".dlq";

    public static final String RK_AUDITORIA_FAILED = "auditoria.failed";
    public static final String RK_METRICAS_FAILED = "metricas.failed";
    public static final String RK_NOTIFICACIONES_FAILED = "notificaciones.failed";

    public static final String RK_PEDIDO_CREAR = "pedido.crear";
    
    public static final String RK_PEDIDO_CREADO = "pedido.creado";
    
    public static final String RK_STOCK_RESERVADO = "pedido.stock.reservado";
    public static final String RK_STOCK_FALLIDO = "pedido.stock.fallido";
    public static final String RK_ESTADO_CANCELADO = "pedido.estado.cancelado";

    @Bean
    public TopicExchange pedidosTopicExchange() {
        return new TopicExchange(TOPIC, true, false);
    }

    @Bean
    public DirectExchange pedidosDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue auditoriaQueue() {
        return durableQueueWithDlx(AUDITORIA, RK_AUDITORIA_FAILED);
    }

    @Bean
    public Queue metricasQueue() {
        return durableQueueWithDlx(METRICAS, RK_METRICAS_FAILED);
    }

    @Bean
    public Queue notificacionesQueue() {
        return durableQueueWithDlx(NOTIFICACIONES, RK_NOTIFICACIONES_FAILED);
    }

    @Bean
    public Queue auditoriaDlq() {
        return new Queue(AUDITORIA_DLQ, true);
    }

    @Bean
    public Queue metricasDlq() {
        return new Queue(METRICAS_DLQ, true);
    }

    @Bean
    public Queue notificacionesDlq() {
        return new Queue(NOTIFICACIONES_DLQ, true);
    }

    @Bean
    public Binding auditoriaBinding() {
        return BindingBuilder.bind(auditoriaQueue()).to(pedidosTopicExchange()).with("pedido.#");
    }

    @Bean
    public Binding metricasBinding() {
        return BindingBuilder.bind(metricasQueue()).to(pedidosTopicExchange()).with("pedido.#");
    }

    @Bean
    public Binding notificacionesStockBinding() 
    {
        return BindingBuilder.bind(notificacionesQueue()).to(pedidosTopicExchange()).with("pedido.stock.#");
    }

    @Bean
    public Binding notificacionesEstadoBinding() {
        return BindingBuilder.bind(notificacionesQueue()).to(pedidosTopicExchange()).with("pedido.estado.#");
    }

    @Bean
    public Binding auditoriaDlqBinding() {
        return BindingBuilder
            .bind(auditoriaDlq())
            .to(pedidosDlx())
            .with(RK_AUDITORIA_FAILED);
    }

    @Bean
    public Binding metricasDlqBinding() {
        return BindingBuilder
            .bind(metricasDlq())
            .to(pedidosDlx())
            .with(RK_METRICAS_FAILED);
    }

    @Bean
    public Binding notificacionesDlqBinding() {
        return BindingBuilder
            .bind(notificacionesDlq())
            .to(pedidosDlx())
            .with(RK_NOTIFICACIONES_FAILED);
    }

    @Bean
    public Binding auditoriaFallidosBinding()
    {
        return BindingBuilder
            .bind(auditoriaQueue())
            .to(pedidosDlx())
            .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding metricasFallidosBinding()
    {
        return BindingBuilder
            .bind(metricasQueue())
            .to(pedidosDlx())
            .with(DLQ_ROUTING_KEY);
    }

    private Queue durableQueueWithDlx(String name, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
            .withArguments(Map.of(
                "x-dead-letter-exchange", DLX,
                "x-dead-letter-routing-key", deadLetterRoutingKey,
                "x-message-ttl", 600000,
                "x-max-length", 1000))
            .build();
    }
}
