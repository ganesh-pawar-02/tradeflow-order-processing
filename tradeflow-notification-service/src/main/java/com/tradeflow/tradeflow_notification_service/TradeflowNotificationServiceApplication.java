package com.tradeflow.tradeflow_notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class TradeflowNotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeflowNotificationServiceApplication.class, args);
	}

}
