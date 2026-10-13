
package neytan.rabbitmq_admin_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RabbitManagementClientConfig {

    @Bean
    public RestClient rabbitManagementRestClient(
        @Value("${spring.rabbitmq.username}") String username,
        @Value("${spring.rabbitmq.password}") String password) {

    return RestClient.builder()
            .defaultHeaders(headers ->
                    headers.setBasicAuth(username, password))
            .build();
}
}
