package neytan.rabbitmq_admin_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OverviewInfo(

        @JsonAlias("queue_totals")
        QueueTotals queueTotals,

        @JsonAlias("object_totals")
        ObjectTotals objectTotals
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QueueTotals(
            long messages,

            @JsonAlias("messages_ready")
            long messagesReady,

            @JsonAlias("messages_unacknowledged")
            long messagesUnacknowledged
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ObjectTotals(
            long consumers,
            long queues,
            long exchanges
    ) {}
}
