package neytan.rabbitmq_admin_service.service;

import neytan.rabbitmq_admin_service.dto.CreateBindingRequest;
import neytan.rabbitmq_admin_service.dto.CreateExchangeRequest;
import neytan.rabbitmq_admin_service.dto.CreateQueueRequest;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

@Service 
public class RabbitAdminService {
    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public void crearCola(CreateQueueRequest request) {
        boolean durable = request.durable() == null
                || request.durable();

        boolean exclusive = Boolean.TRUE.equals(
                request.exclusive());

        boolean autoDelete = Boolean.TRUE.equals(
                request.autoDelete());

        Queue queue = new Queue(
                request.name(),
                durable,
                exclusive,
                autoDelete
        );

        amqpAdmin.declareQueue(queue);
    }

    public void eliminarCola(String name) {
        amqpAdmin.deleteQueue(name);
    }

    public void crearExchange(CreateExchangeRequest request) {
        String type = request.type().toLowerCase();
        boolean durable = request.durable() == null
                || request.durable();

        switch (type) {
            case "direct" ->
                amqpAdmin.declareExchange(
                    new DirectExchange(
                        request.name(), durable, false));

            case "topic" ->
                amqpAdmin.declareExchange(
                    new TopicExchange(
                        request.name(), durable, false));

            case "fanout" ->
                amqpAdmin.declareExchange(
                    new FanoutExchange(
                        request.name(), durable, false));

            default ->
                throw new IllegalArgumentException(
                    "Tipo de exchange no permitido");
        }
    }

    public void eliminarExchange(String name) {
        amqpAdmin.deleteExchange(name);
    }

    public void crearBinding(CreateBindingRequest request) {
        Binding binding = new Binding(
                request.queueName(),
                Binding.DestinationType.QUEUE,
                request.exchangeName(),
                request.routingKey() == null
                        ? ""
                        : request.routingKey(),
                null
        );

        amqpAdmin.declareBinding(binding);
    }

    public void eliminarBinding(CreateBindingRequest request) {
        Binding binding = new Binding(
                request.queueName(),
                Binding.DestinationType.QUEUE,
                request.exchangeName(),
                request.routingKey() == null
                        ? ""
                        : request.routingKey(),
                null
        );

        amqpAdmin.removeBinding(binding);
    }

}
