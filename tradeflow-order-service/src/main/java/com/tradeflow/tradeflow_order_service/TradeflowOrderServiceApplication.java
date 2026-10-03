package com.tradeflow.tradeflow_order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class TradeflowOrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeflowOrderServiceApplication.class, args);
	}

}
