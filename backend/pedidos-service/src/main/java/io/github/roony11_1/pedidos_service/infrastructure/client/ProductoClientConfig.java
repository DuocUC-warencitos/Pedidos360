package io.github.roony11_1.pedidos_service.infrastructure.client;

import feign.Logger;
import feign.RequestInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductoClientConfig
{
    @Bean
    Logger.Level feignLoggerLevel()
    {
        return Logger.Level.BASIC;
    }

    @Bean
    RequestInterceptor internalApiKeyRequestInterceptor(
        @Value("${app.producto-service.internal-api-key:}") String internalApiKey)
    {
        return template ->
        {
            if (internalApiKey != null && !internalApiKey.isBlank())
            {
                template.header("X-Internal-Api-Key", internalApiKey);
            }
        };
    }
}