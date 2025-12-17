package com.banking.health;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HealthAggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthAggregatorApplication.class, args);
    }
}
