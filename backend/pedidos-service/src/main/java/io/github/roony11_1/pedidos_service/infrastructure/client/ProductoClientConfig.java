package io.github.roony11_1.pedidos_service.infrastructure.client;

import feign.Logger;

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
}