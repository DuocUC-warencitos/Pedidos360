package io.github.roony11_1.analytics_service.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.errors.MissingSourceTopicException;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.apache.kafka.streams.state.Stores;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;
import org.springframework.kafka.core.CleanupConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.roony11_1.analytics_service.dto.ProductoAuditoriaEvent;
import io.github.roony11_1.analytics_service.dto.StockItemProcesable;
import io.github.roony11_1.analytics_service.dto.StockOperacionEvento;
import io.github.roony11_1.analytics_service.streams.AuditoriaItem;
import io.github.roony11_1.analytics_service.streams.InventarioProducto;
import io.github.roony11_1.analytics_service.streams.MovimientoProductoDia;
import io.github.roony11_1.analytics_service.streams.ProductoProcessor;
import io.github.roony11_1.analytics_service.streams.StockItemProcessor;
import io.github.roony11_1.analytics_service.streams.StoreNames;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    }

    /**
     * Reemplaza al configurer que crea Spring Boot para conservar la misma
     * configuracion de auto-startup/cleanup y ademas tolerar que los topics de
     * origen aparezcan un instante despues del arranque del servicio.
     *
     * Sin esto, Kafka Streams recibe MissingSourceTopicException, se apaga y
     * queda en estado ERROR para siempre, por lo que todas las consultas de
     * analiticas responden HTTP 500.
     */
    @Bean
    public StreamsBuilderFactoryBeanConfigurer analyticsStreamsConfigurer(
        KafkaProperties kafkaProperties) 
    {
        KafkaProperties.Streams streams = kafkaProperties.getStreams();
        KafkaProperties.Cleanup cleanup = streams.getCleanup();
        CleanupConfig cleanupConfig = new CleanupConfig(
            cleanup.isOnStartup(),
            cleanup.isOnShutdown()
        );

        return factoryBean -> 
        {
            factoryBean.setAutoStartup(streams.isAutoStartup());
            factoryBean.setCleanupConfig(cleanupConfig);
            factoryBean.setStreamsUncaughtExceptionHandler(
                KafkaStreamsConfig::reintentarSiFaltanTopics
            );
        };
    }

    private static StreamThreadExceptionResponse reintentarSiFaltanTopics(
        Throwable ex) 
    {
        if (ex instanceof MissingSourceTopicException) 
        {
            return StreamThreadExceptionResponse.REPLACE_THREAD;
        }

        return StreamThreadExceptionResponse.SHUTDOWN_CLIENT;
    }

    @Bean
    public KStream<String, ProductoAuditoriaEvent> analiticaTopology(
        StreamsBuilder builder,
        ObjectMapper objectMapper
    ) {
        JsonSerde<ProductoAuditoriaEvent> productoSerde =
            new JsonSerde<>(objectMapper, ProductoAuditoriaEvent.class);
        JsonSerde<StockOperacionEvento> stockSerde =
            new JsonSerde<>(objectMapper, StockOperacionEvento.class);
        JsonSerde<StockItemProcesable> stockItemSerde =
            new JsonSerde<>(objectMapper, StockItemProcesable.class);

        agregarStores(builder, objectMapper);

        KStream<String, ProductoAuditoriaEvent> productos = builder.stream(
            KafkaTopics.PRODUCTOS,
            Consumed.with(Serdes.String(), productoSerde)
        );

        KStream<String, StockOperacionEvento> stock = builder.stream(
            KafkaTopics.STOCK,
            Consumed.with(Serdes.String(), stockSerde)
        );

        productos.process(
            () -> new ProductoProcessor(
                StoreNames.INVENTARIO,
                StoreNames.PRODUCTO_DEDUP,
                StoreNames.AUDITORIA
            ),
            Named.as("producto-processor"),
            StoreNames.INVENTARIO,
            StoreNames.PRODUCTO_DEDUP,
            StoreNames.AUDITORIA
        );

        KStream<String, StockItemProcesable> stockItems = stock.flatMap(
            (key, value) -> {
                if (value == null || value.items() == null) {
                    return List.of();
                }

                return value.items().stream()
                    .map(item -> KeyValue.pair(
                        item.productoId().toString(),
                        new StockItemProcesable(
                            value.eventoId(),
                            value.tipo(),
                            value.operacionId(),
                            value.pedidoId(),
                            value.correlationId(),
                            value.ocurridoEn(),
                            item
                        )
                    ))
                    .toList();
            }
        );

        stockItems
            .repartition(
                Repartitioned.<String, StockItemProcesable>as("stock-items-repartition")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(stockItemSerde)
                    .withNumberOfPartitions(3)
            )
            .process(
                () -> new StockItemProcessor(
                    StoreNames.INVENTARIO,
                    StoreNames.STOCK_DEDUP,
                    StoreNames.MOVIMIENTOS,
                    StoreNames.AUDITORIA
                ),
                Named.as("stock-item-processor"),
                StoreNames.INVENTARIO,
                StoreNames.STOCK_DEDUP,
                StoreNames.MOVIMIENTOS,
                StoreNames.AUDITORIA
            );

        return productos;
    }

    private void agregarStores(
        StreamsBuilder builder,
        ObjectMapper objectMapper
    ) {
        builder.addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(StoreNames.INVENTARIO),
                Serdes.String(),
                new JsonSerde<>(objectMapper, InventarioProducto.class)
            ).withLoggingEnabled(Map.of())
        );

        builder.addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(StoreNames.PRODUCTO_DEDUP),
                Serdes.String(),
                Serdes.String()
            ).withLoggingEnabled(Map.of())
        );

        builder.addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(StoreNames.STOCK_DEDUP),
                Serdes.String(),
                Serdes.String()
            ).withLoggingEnabled(Map.of())
        );

        builder.addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(StoreNames.MOVIMIENTOS),
                Serdes.String(),
                new JsonSerde<>(objectMapper, MovimientoProductoDia.class)
            ).withLoggingEnabled(Map.of())
        );

        builder.addStateStore(
            Stores.windowStoreBuilder(
                Stores.persistentWindowStore(
                    StoreNames.AUDITORIA,
                    Duration.ofDays(90),
                    Duration.ofDays(1),
                    false
                ),
                Serdes.String(),
                new JsonSerde<>(objectMapper, AuditoriaItem.class)
            ).withLoggingEnabled(Map.of())
        );
    }
}
