package com.tradeflow.tradeflow_product_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TradeflowProductServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeflowProductServiceApplication.class, args);
	}

}
