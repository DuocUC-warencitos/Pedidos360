
package neytan.rabbitmq_admin_service.service;

import neytan.rabbitmq_admin_service.dto.BindingInfo;
import neytan.rabbitmq_admin_service.dto.CreateBindingRequest;
import neytan.rabbitmq_admin_service.dto.CreateExchangeRequest;
import neytan.rabbitmq_admin_service.dto.CreateQueueRequest;
import neytan.rabbitmq_admin_service.dto.ExchangeInfo;
import neytan.rabbitmq_admin_service.dto.OverviewInfo;
import neytan.rabbitmq_admin_service.dto.PageResponse;
import neytan.rabbitmq_admin_service.dto.QueueInfo;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;
    private final RabbitManagementService managementService;

    public RabbitAdminService(
            AmqpAdmin amqpAdmin,
            RabbitManagementService managementService) {
        this.amqpAdmin = amqpAdmin;
        this.managementService = managementService;
    }

    public PageResponse<QueueInfo> listarColas(
            int page,
            int size,
            String name) {
        return managementService.listarColasPagina(page, size, name);
    }

    public List<ExchangeInfo> listarExchanges() {
        return managementService.listarExchanges();
    }

    public List<BindingInfo> listarBindings() {
        return managementService.listarBindings();
    }

    public OverviewInfo obtenerOverview() {
        return managementService.obtenerOverview();
    }

    public void crearCola(CreateQueueRequest request) {
        String name = normalizarNombreCola(request.name());

        boolean durable = request.durable() == null
                || request.durable();
        boolean exclusive = Boolean.TRUE.equals(request.exclusive());
        boolean autoDelete = Boolean.TRUE.equals(request.autoDelete());

        try {
            amqpAdmin.declareQueue(
                    new Queue(name, durable, exclusive, autoDelete));
        } catch (AmqpException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo crear la cola. Verifica su configuración.",
                    ex);
        }
    }

    public void eliminarCola(String name) {
    final String nombre = validarNombre(name);

    boolean existe = managementService.listarTodasLasColas().stream()
            .anyMatch(queue -> queue.name().equals(nombre));

    if (!existe) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "La cola solicitada no existe.");
    }

    try {
        amqpAdmin.deleteQueue(nombre);
    } catch (AmqpException ex) {
        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "RabbitMQ no pudo eliminar la cola.",
                ex);
    }
}

    public void crearExchange(CreateExchangeRequest request) {
        String name = validarNombre(request.name());
        String type = request.type().toLowerCase(Locale.ROOT);
        boolean durable = request.durable() == null
                || request.durable();

        try {
            switch (type) {
                case "direct" ->
                        amqpAdmin.declareExchange(
                                new DirectExchange(name, durable, false));

                case "topic" ->
                        amqpAdmin.declareExchange(
                                new TopicExchange(name, durable, false));

                case "fanout" ->
                        amqpAdmin.declareExchange(
                                new FanoutExchange(name, durable, false));

                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tipo de exchange no permitido.");
            }
        } catch (AmqpException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo crear el exchange. Verifica su configuración.",
                    ex);
        }
    }

    public void eliminarExchange(String name) {
        final String nombre = validarNombre(name);

        boolean existe = managementService.listarExchanges().stream()
                .anyMatch(exchange -> exchange.name().equals(nombre));

        if (!existe) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El exchange solicitado no existe.");
        }

        try {
            amqpAdmin.deleteExchange(nombre);
        } catch (AmqpException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "RabbitMQ no pudo eliminar el exchange.",
                    ex);
        }
    }

    public void crearBinding(CreateBindingRequest request) {
        String queueName = validarNombre(request.queueName());
        String exchangeName = validarNombre(request.exchangeName());
        String routingKey = request.routingKey() == null
                ? ""
                : request.routingKey().trim();

        boolean queueExists = managementService.listarTodasLasColas().stream()
                .anyMatch(queue -> queue.name().equals(queueName));

        if (!queueExists) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "La cola seleccionada no existe.");
        }

        boolean exchangeExists = managementService.listarExchanges().stream()
                .anyMatch(exchange -> exchange.name().equals(exchangeName));

        if (!exchangeExists) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El exchange seleccionado no existe.");
        }

        try {
            amqpAdmin.declareBinding(new Binding(
                    queueName,
                    Binding.DestinationType.QUEUE,
                    exchangeName,
                    routingKey,
                    null));
        } catch (AmqpException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo crear el binding.",
                    ex);
        }
    }

    public void eliminarBinding(CreateBindingRequest request) {
        String queueName = validarNombre(request.queueName());
        String exchangeName = validarNombre(request.exchangeName());
        String routingKey = request.routingKey() == null
                ? ""
                : request.routingKey().trim();

        boolean existe = managementService.listarBindings().stream()
                .anyMatch(binding ->
                        binding.destination().equals(queueName)
                        && "queue".equalsIgnoreCase(binding.destinationType())
                        && binding.source().equals(exchangeName)
                        && binding.routingKey().equals(routingKey));

        if (!existe) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El binding solicitado no existe.");
        }

        try {
            amqpAdmin.removeBinding(new Binding(
                    queueName,
                    Binding.DestinationType.QUEUE,
                    exchangeName,
                    routingKey,
                    null));
        } catch (AmqpException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "RabbitMQ no pudo eliminar el binding.",
                    ex);
        }
    }

    private String validarNombre(String name) {
        if (name == null
                || !name.matches("^[a-zA-Z0-9._-]{1,100}$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nombre de recurso inválido.");
        }

        return name;
    }
    private String normalizarNombreCola(String name) {
    if (name == null) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El nombre de la cola es obligatorio.");
    }

    String limpio = name.trim();

    if (limpio.startsWith("pedidos360.")
            && limpio.endsWith(".queue")) {
        if (!limpio.matches(
                "^pedidos360\\.[a-zA-Z0-9_-]{1,80}\\.queue$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre completo de la cola no es válido.");
        }

        return limpio;
    }

    if (!limpio.matches("^[a-zA-Z0-9_-]{1,80}$")) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ingresa un nombre corto válido para la cola.");
    }

    return "pedidos360." + limpio + ".queue";
}
}
