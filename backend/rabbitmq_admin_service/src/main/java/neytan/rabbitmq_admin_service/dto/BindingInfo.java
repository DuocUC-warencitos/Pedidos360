
package neytan.rabbitmq_admin_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BindingInfo(
        String source,
        String destination,

        @JsonAlias("destination_type")
        String destinationType,

        @JsonAlias("routing_key")
        String routingKey
) {}
