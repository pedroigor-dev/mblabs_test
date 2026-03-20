package com.mblabs.demo.service;

import com.mblabs.demo.config.RabbitMQConfig;
import com.mblabs.demo.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ProductNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ProductNotificationService.class);

    private final RabbitTemplate rabbitTemplate;

    public ProductNotificationService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Async
    public void notifyProductCreated(Product product) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    "Product created: " + product.getName());
        } catch (AmqpException e) {
            log.warn("Failed to send notification for product {}: {}", product.getId(), e.getMessage());
        }
    }
}
