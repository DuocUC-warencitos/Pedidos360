package io.github.roony11_1.pedidos_service.infrastructure.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig
{
    @Bean("pedidoExecutor")
    public TaskExecutor pedidoExecutor()
    {
        var ex = new ThreadPoolTaskExecutor();

        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(16);
        ex.setQueueCapacity(200);
        ex.setKeepAliveSeconds(60);
        ex.setThreadNamePrefix("pedido-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(30);
        ex.initialize();

        return ex;
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter()
    {
        return new JacksonJsonMessageConverter();
    }

    @Bean("pedidoRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory pedidoRabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        TaskExecutor pedidoExecutor,
        JacksonJsonMessageConverter jacksonJsonMessageConverter)
    {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setTaskExecutor(pedidoExecutor);
        factory.setMessageConverter(jacksonJsonMessageConverter);
        factory.setConcurrentConsumers(4);
        factory.setMaxConcurrentConsumers(16);
        factory.setPrefetchCount(10);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}
