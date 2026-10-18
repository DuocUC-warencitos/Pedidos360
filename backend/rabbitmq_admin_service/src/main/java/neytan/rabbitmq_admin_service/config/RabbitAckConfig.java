package neytan.rabbitmq_admin_service.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitAckConfig
{
    @Bean("notificacionesRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory notificacionesRabbitListenerContainerFactory(
        ConnectionFactory connectionFactory)
    {
        var factory = new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}
