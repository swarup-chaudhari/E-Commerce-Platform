package com.zaalima.ecommerce.inventory;

import com.zaalima.ecommerce.common.CommonKafkaConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@Import(CommonKafkaConfig.class)
@EnableJpaRepositories(basePackages = "com.zaalima.ecommerce.inventory.repository.jpa")
@EnableMongoRepositories(basePackages = "com.zaalima.ecommerce.inventory.repository.mongo")
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
