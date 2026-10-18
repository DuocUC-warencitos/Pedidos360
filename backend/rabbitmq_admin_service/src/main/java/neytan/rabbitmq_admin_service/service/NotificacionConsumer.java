
package neytan.rabbitmq_admin_service.service;
import neytan.rabbitmq_admin_service.config.RabbitTopologyConfig;
import neytan.rabbitmq_admin_service.model.Notificacion;
import neytan.rabbitmq_admin_service.repository.NotificacionRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificacionConsumer {

    private final ObjectMapper objectMapper;
    private final NotificacionRepository repository;

    public NotificacionConsumer(
            ObjectMapper objectMapper,
            NotificacionRepository repository) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    @RabbitListener(queues = RabbitTopologyConfig.NOTIFICACIONES)
    @Transactional
    public void recibir(Message message) throws Exception {
        String routingKey =
            message.getMessageProperties().getReceivedRoutingKey();

        JsonNode evento = objectMapper.readTree(message.getBody());

        String userId = texto(evento, "userId");
        String pedidoId = texto(evento, "pedidoId");

        if (userId == null || pedidoId == null) {
            throw new IllegalArgumentException(
                "El evento no contiene userId o pedidoId");
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
                return;
            }
        }

        if (estado == null) {
            throw new IllegalArgumentException(
                "No se pudo determinar el estado del pedido");
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

        if (mensaje != null) {
            repository.save(new Notificacion(userId, mensaje));
        }
    }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull()
            ? null
            : valor.asText();
    }
}
