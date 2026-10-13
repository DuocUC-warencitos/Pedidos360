
package neytan.rabbitmq_admin_service.service;

import neytan.rabbitmq_admin_service.dto.BindingInfo;
import neytan.rabbitmq_admin_service.dto.ExchangeInfo;
import neytan.rabbitmq_admin_service.dto.QueueInfo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

@Service
public class RabbitManagementService {

    private final RestClient client;
    private final String managementUrl;

    public RabbitManagementService(
            RestClient rabbitManagementRestClient,
            @Value("${rabbitmq.management-url:http://localhost:15672}")
            String managementUrl) {

        this.client = rabbitManagementRestClient;
        this.managementUrl = managementUrl.replaceAll("/+$", "");
    }

    private URI uri(String path) {
        return URI.create(managementUrl + path);
    }

    public List<QueueInfo> listarColas() {
        List<QueueInfo> result = client.get()
                .uri(uri("/api/queues/%2F"))
                .retrieve()
                .body(new ParameterizedTypeReference<List<QueueInfo>>() {});

        return result == null ? List.of() : result;
    }

    public List<ExchangeInfo> listarExchanges() {
        List<ExchangeInfo> result = client.get()
                .uri(uri("/api/exchanges/%2F"))
                .retrieve()
                .body(new ParameterizedTypeReference<List<ExchangeInfo>>() {});

        return result == null ? List.of() : result;
    }

    public List<BindingInfo> listarBindings() {
        List<BindingInfo> result = client.get()
                .uri(uri("/api/bindings/%2F"))
                .retrieve()
                .body(new ParameterizedTypeReference<List<BindingInfo>>() {});

        return result == null ? List.of() : result;
    }
}
