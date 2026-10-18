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
        return durableQueueWithDlx(AUDITORIA);
    }

    @Bean
    public Queue metricasQueue() {
        return durableQueueWithDlx(METRICAS);
    }

    @Bean
    public Queue notificacionesQueue() {
        return durableQueueWithDlx(NOTIFICACIONES);
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
    public Binding auditoriaFallidosBinding()
    {
        return BindingBuilder
            .bind(auditoriaQueue())
            .to(pedidosDlx())
            .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding notificacionesEstadoBinding() {
        return BindingBuilder.bind(notificacionesQueue()).to(pedidosTopicExchange()).with("pedido.estado.#");
    }

    private Queue durableQueueWithDlx(String name) {
        return QueueBuilder.durable(name)
            .withArguments(Map.of(
                "x-dead-letter-exchange", DLX,
                "x-dead-letter-routing-key", DLQ_ROUTING_KEY))
            .build();
    }
}
