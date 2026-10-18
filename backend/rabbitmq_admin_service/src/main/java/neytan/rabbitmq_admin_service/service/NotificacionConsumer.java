
package neytan.rabbitmq_admin_service.service;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import neytan.rabbitmq_admin_service.config.RabbitTopologyConfig;
import neytan.rabbitmq_admin_service.model.Notificacion;
import neytan.rabbitmq_admin_service.repository.NotificacionRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class NotificacionConsumer {

    private static final Logger log =
        LoggerFactory.getLogger(NotificacionConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificacionRepository repository;

    public NotificacionConsumer(
            ObjectMapper objectMapper,
            NotificacionRepository repository) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    @RabbitListener(
        queues = RabbitTopologyConfig.NOTIFICACIONES,
        containerFactory = "notificacionesRabbitListenerContainerFactory")
    public void recibir(Message message, Channel channel) throws IOException {
        String routingKey =
            message.getMessageProperties().getReceivedRoutingKey();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            if (procesar(message, routingKey)) {
                channel.basicAck(deliveryTag, false);
            } else {
                log.warn("Evento descartado hacia DLQ rk={}", routingKey);
                channel.basicNack(deliveryTag, false, false);
            }
        } catch (Exception ex) {
            log.error("Error procesando evento; enviado a DLQ rk={}: {}",
                routingKey, ex.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private boolean procesar(Message message, String routingKey) {
        JsonNode evento;

        try {
            evento = objectMapper.readTree(message.getBody());
        } catch (Exception ex) {
            log.warn("Evento descartado (payload ilegible) rk={}: {}",
                routingKey, ex.getMessage());
            return false;
        }

        String userId = texto(evento, "userId");
        String pedidoId = texto(evento, "pedidoId");

        if (userId == null || pedidoId == null) {
            log.warn("Evento descartado (sin userId o pedidoId) rk={}",
                routingKey);
            return false;
        }

        String estado;
        String detalle = "";

        switch (routingKey) {
            case "pedido.stock.reservado" ->
                estado = "ACEPTADO";

            case "pedido.stock.fallido" -> {
                estado = "STOCK_FALLIDO";
                detalle = texto(evento, "error");
            }

            case "pedido.estado.cancelado" -> {
                estado = "CANCELADO";
                detalle = texto(evento, "motivo");
            }

            case "pedido.estado.actualizado" -> {
                estado = texto(evento, "estado");
            }

            default -> {
                return false;
            }
        }

        if (estado == null) {
            log.warn("Evento descartado (sin estado) rk={}", routingKey);
            return false;
        }

        String mensaje = switch (estado) {
            case "CREADO" ->
                "Recibimos tu pedido " + pedidoId + ".";
            case "ACEPTADO" ->
                "Tu pedido " + pedidoId
                    + " fue aceptado y el stock está reservado.";
            case "STOCK_FALLIDO" ->
                "No se pudo procesar el pedido " + pedidoId
                    + (detalle == null ? "." : ": " + detalle);
            case "EN_PREPARACION" ->
                "Tu pedido " + pedidoId + " está en preparación.";
            case "DESPACHADO" ->
                "Tu pedido " + pedidoId + " fue despachado.";
            case "ENTREGADO" ->
                "Tu pedido " + pedidoId + " fue entregado.";
            case "CANCELADO" ->
                "Tu pedido " + pedidoId + " fue cancelado.";
            default -> null;
        };

        if (mensaje == null) {
            return false;
        }

        guardar(userId, mensaje, eventoId(message, routingKey));
        return true;
    }

    private void guardar(String userId, String mensaje, String eventoId) {
        if (repository.existsByEventoId(eventoId)) {
            log.info("Notificación duplicada ignorada eventoId={}", eventoId);
            return;
        }

        try {
            repository.save(new Notificacion(userId, mensaje, eventoId));
        } catch (DataIntegrityViolationException ex) {
            log.info("Notificación duplicada ignorada (constraint) eventoId={}",
                eventoId);
        }
    }

    private String eventoId(Message message, String routingKey) {
        String messageId = message.getMessageProperties().getMessageId();

        if (messageId == null || messageId.isBlank())
            messageId = UUID.nameUUIDFromBytes(message.getBody()).toString();

        return routingKey + ":" + messageId;
    }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull()
            ? null
            : valor.asText();
    }
}
