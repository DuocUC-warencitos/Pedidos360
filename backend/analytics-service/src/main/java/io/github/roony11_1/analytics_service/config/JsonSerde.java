package io.github.roony11_1.analytics_service.config;

import java.io.IOException;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSerde<T> implements Serde<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> type;

    public JsonSerde(ObjectMapper objectMapper, Class<T> type) {
        this.objectMapper = objectMapper;
        this.type = type;
    }

    @Override
    public Serializer<T> serializer() {
        return (topic, data) -> {
            if (data == null) {
                return null;
            }

            try {
                return objectMapper.writeValueAsBytes(data);
            } catch (IOException ex) {
                throw new IllegalArgumentException(
                    "No se pudo serializar " + type.getSimpleName(),
                    ex
                );
            }
        };
    }

    @Override
    public Deserializer<T> deserializer() {
        return (topic, data) -> {
            if (data == null) {
                return null;
            }

            try {
                return objectMapper.readValue(data, type);
            } catch (IOException ex) {
                throw new IllegalArgumentException(
                    "No se pudo deserializar " + type.getSimpleName(),
                    ex
                );
            }
        };
    }
}
