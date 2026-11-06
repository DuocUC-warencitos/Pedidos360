package io.github.roony11_1.producto_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_PRODUCTOS =
        "productos360.eventos.productos";

    public static final String TOPIC_STOCK =
        "productos360.eventos.stock";

    public static final String TOPIC_AUDITORIA =
        "productos360.eventos.auditoria";

    @Value("${app.kafka.replication-factor:1}")
    private int replicationFactor;

    @Bean
    public NewTopic productosTopic() {
        return TopicBuilder.name(TOPIC_PRODUCTOS)
            .partitions(3)
            .replicas(replicationFactor)
            .build();
    }

    @Bean
    public NewTopic stockTopic() {
        return TopicBuilder.name(TOPIC_STOCK)
            .partitions(3)
            .replicas(replicationFactor)
            .build();
    }

    @Bean
    public NewTopic auditoriaTopic() {
        return TopicBuilder.name(TOPIC_AUDITORIA)
            .partitions(3)
            .replicas(replicationFactor)
            .build();
    }
}
