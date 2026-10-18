
package neytan.rabbitmq_admin_service.service;

import neytan.rabbitmq_admin_service.dto.BindingInfo;
import neytan.rabbitmq_admin_service.dto.ExchangeInfo;
import neytan.rabbitmq_admin_service.dto.OverviewInfo;
import neytan.rabbitmq_admin_service.dto.PageResponse;
import neytan.rabbitmq_admin_service.dto.QueueInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RabbitManagementService {

    private static final int TAMANO_MAXIMO_PAGINA = 500;

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

    public List<QueueInfo> listarTodasLasColas() {
        List<QueueInfo> result = client.get()
                .uri(uri("/api/queues/%2F"))
                .retrieve()
                .body(new ParameterizedTypeReference<List<QueueInfo>>() {});

        return result == null ? List.of() : result;
    }

    public PageResponse<QueueInfo> listarColasPagina(
            int page,
            int size,
            String name) {

        int pagina = Math.max(page, 0);
        int tamano = Math.min(Math.max(size, 1), TAMANO_MAXIMO_PAGINA);

        StringBuilder url = new StringBuilder(managementUrl)
                .append("/api/queues/%2F")
                .append("?page=").append(pagina + 1)
                .append("&page_size=").append(tamano)
                .append("&pagination=true");

        if (name != null && !name.isBlank()) {
            url.append("&name=")
                    .append(UriUtils.encodeQueryParam(
                            name.trim(),
                            StandardCharsets.UTF_8));
        }

        try {
            RabbitQueuePage respuesta = client.get()
                    .uri(URI.create(url.toString()))
                    .retrieve()
                    .body(RabbitQueuePage.class);

            if (respuesta == null) {
                return PageResponse.of(List.of(), 0, 0, pagina, tamano);
            }

            int totalPaginas = Math.max(respuesta.pageCount(), 0);

            return PageResponse.of(
                    respuesta.items(),
                    respuesta.filteredCount(),
                    totalPaginas,
                    pagina,
                    tamano);

        } catch (HttpClientErrorException.BadRequest ex) {
            // RabbitMQ responde 400 "Page out of range" si la página
            // solicitada ya no existe (por ejemplo, tras eliminar colas).
            return PageResponse.of(List.of(), 0, 0, pagina, tamano);
        }
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

    public OverviewInfo obtenerOverview() {
        OverviewInfo result = client.get()
                .uri(uri("/api/overview"))
                .retrieve()
                .body(OverviewInfo.class);

        return result == null
                ? new OverviewInfo(null, null)
                : result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RabbitQueuePage(

            @JsonProperty("total_count")
            long totalCount,

            @JsonProperty("item_count")
            long itemCount,

            @JsonProperty("filtered_count")
            long filteredCount,

            @JsonProperty("page")
            int page,

            @JsonProperty("page_size")
            int pageSize,

            @JsonProperty("page_count")
            int pageCount,

            @JsonProperty("items")
            List<QueueInfo> items
    ) {}
}
