package io.github.roony11_1.analytics_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declara los topics de entrada del servicio de analiticas.
 *
 * Kafka Streams falla de forma definitiva (estado ERROR) si al arrancar no
 * existen sus topics de origen, y no vuelve a intentarlo por si solo. Al
 * declararlos aqui, el KafkaAdmin de Spring los crea durante el arranque del
 * contexto, antes de que el StreamsBuilderFactoryBean inicie Kafka Streams.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.replication-factor:1}")
    private int replicationFactor;

    @Bean
    public NewTopic productosEventosTopic() {
        return TopicBuilder.name(KafkaTopics.PRODUCTOS)
            .partitions(3)
            .replicas(replicationFactor)
            .build();
    }

    @Bean
    public NewTopic stockEventosTopic() {
        return TopicBuilder.name(KafkaTopics.STOCK)
            .partitions(3)
            .replicas(replicationFactor)
            .build();
    }
}
