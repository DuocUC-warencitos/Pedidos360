package neytan.rabbitmq_admin_service.listener;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import neytan.rabbitmq_admin_service.config.RabbitTopologyConfig;
import neytan.rabbitmq_admin_service.model.ProductoComprado;
import neytan.rabbitmq_admin_service.service.MetricasService;
import tools.jackson.databind.ObjectMapper;

@Component
public class RabbitMetricasListener {

    private static final Logger log = LoggerFactory.getLogger(RabbitMetricasListener.class);

    private final MetricasService metricasService;
    private final ObjectMapper objectMapper;

    public RabbitMetricasListener(MetricasService metricasService, ObjectMapper objectMapper) {
        this.metricasService = metricasService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitTopologyConfig.METRICAS)
    public void onMessage(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        try {
            metricasService.registrarEvento(routingKey);

            if (RabbitTopologyConfig.RK_PEDIDO_CREADO.equals(routingKey)) {
                registrarProductos(new String(message.getBody(), StandardCharsets.UTF_8));
            }
        } catch (Exception ex) {
            log.error("No se pudo registrar métrica rk={}: {}", routingKey, ex.getMessage(), ex);
        }
    }

    private void registrarProductos(String payload) {
        Map<?, ?> data = objectMapper.readValue(payload, Map.class);
        Object itemsRaw = data.get("items");

        if (!(itemsRaw instanceof List<?> items) || items.isEmpty()) {
            return;
        }

        LocalDate hoy = LocalDate.now();
        List<ProductoComprado> filas = new ArrayList<>();

        for (Object itemRaw : items) {
            if (!(itemRaw instanceof Map<?, ?> item)) {
                continue;
            }

            Object productoId = item.get("productoId");
            Object cantidad = item.get("cantidad");

            if (productoId == null || !(cantidad instanceof Number numero)) {
                continue;
            }

            ProductoComprado fila = new ProductoComprado();
            fila.setProductoId(UUID.fromString(productoId.toString()));
            fila.setCantidad(numero.intValue());
            fila.setFecha(hoy);
            filas.add(fila);
        }

        if (!filas.isEmpty()) {
            metricasService.registrarProductos(filas);
        }
    }
}
