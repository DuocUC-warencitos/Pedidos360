package io.github.roony11_1.pedidos_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "io.github.roony11_1.pedidos_service.infrastructure.client")
public class PedidosServiceApplication 
{
	public static void main(String[] args) 
	{
		SpringApplication.run(PedidosServiceApplication.class, args);
	}
}
