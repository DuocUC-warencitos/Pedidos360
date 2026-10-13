
package neytan.rabbitmq_admin_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QueueInfo(
        String name,
        boolean durable,
        boolean exclusive,

        @JsonProperty("auto_delete")
        boolean autoDelete,

        int messages,
        int consumers
) {}
