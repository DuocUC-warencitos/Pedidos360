
package neytan.rabbitmq_admin_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeInfo(
        String name,
        String type,
        boolean durable,

        @JsonProperty("auto_delete")
        boolean autoDelete
) {}
