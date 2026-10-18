package neytan.rabbitmq_admin_service.listener;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import neytan.rabbitmq_admin_service.config.RabbitTopologyConfig;
import neytan.rabbitmq_admin_service.model.EventoAuditoria;
import neytan.rabbitmq_admin_service.repository.EventoAuditoriaRepository;
import tools.jackson.databind.ObjectMapper;

@Component
public class RabbitAuditoriaListener {

    private static final Logger log = LoggerFactory.getLogger(RabbitAuditoriaListener.class);

    private final EventoAuditoriaRepository repository;
    private final ObjectMapper objectMapper;

    public RabbitAuditoriaListener(EventoAuditoriaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitTopologyConfig.AUDITORIA)
    public void onMessage(Message message) {
        var props = message.getMessageProperties();

        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<?, ?> data = objectMapper.readValue(payload, Map.class);

            var evento = new EventoAuditoria();
            evento.setRoutingKey(props.getReceivedRoutingKey());
            evento.setMessageId(props.getMessageId());
            evento.setCorrelationId(props.getCorrelationId());
            evento.setPedidoId(asString(data.get("pedidoId")));
            evento.setJobId(asString(data.get("jobId")));
            evento.setPayload(payload);
            evento.setRecibidoAt(Instant.now());

            repository.save(evento);
        } catch (Exception ex) {
            log.error("No se pudo persistir evento de auditoría rk={}: {}",
                props.getReceivedRoutingKey(), ex.getMessage(), ex);
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
