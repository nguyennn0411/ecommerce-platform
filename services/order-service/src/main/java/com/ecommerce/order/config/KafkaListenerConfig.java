package com.ecommerce.order.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
@ConditionalOnProperty(prefix = "ecommerce.kafka", name = "enabled", havingValue = "true")
public class KafkaListenerConfig {
}
