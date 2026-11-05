package io.github.roony11_1.producto_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_AUDITORIA =
        "pedidos360.productos.auditoria";

    public static final String TOPIC_REPORTERIA =
        "pedidos360.eventos.reporteria";

    @Value("${app.kafka.replication-factor:1}")
    private int replicationFactor;

    @Bean
    public NewTopic productosAuditoriaTopic() {
        return TopicBuilder.name(TOPIC_AUDITORIA)
            .partitions(3)
            .replicas(replicationFactor)
            .build();
    }

    @Bean
    public NewTopic eventosReporteriaTopic() {
        return TopicBuilder.name(TOPIC_REPORTERIA)
            .partitions(3)
            .replicas(replicationFactor)
            .build();
    }
}