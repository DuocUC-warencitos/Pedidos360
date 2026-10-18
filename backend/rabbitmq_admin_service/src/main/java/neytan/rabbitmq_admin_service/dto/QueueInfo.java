
package neytan.rabbitmq_admin_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QueueInfo(
        String name,
        boolean durable,
        boolean exclusive,

        @JsonAlias("auto_delete")
        boolean autoDelete,

        int messages,

        @JsonAlias("messages_ready")
        int messagesReady,

        @JsonAlias("messages_unacknowledged")
        int messagesUnacknowledged,

        int consumers
) {}
