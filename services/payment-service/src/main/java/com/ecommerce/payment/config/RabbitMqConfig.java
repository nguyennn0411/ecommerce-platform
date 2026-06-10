package com.ecommerce.payment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ECOMMERCE_EXCHANGE = "ecommerce.exchange";
    public static final String PAYMENT_CREATED_ROUTING_KEY = "payment.created";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String PAYMENT_CANCELLED_ROUTING_KEY = "payment.cancelled";
    public static final String PAYMENT_REFUNDED_ROUTING_KEY = "payment.refunded";

    @Bean
    public TopicExchange ecommerceExchange() {
        return new TopicExchange(ECOMMERCE_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         @Qualifier("rabbitMessageConverter") MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            @Qualifier("rabbitMessageConverter") MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        return factory;
    }

    @Bean
    public Queue paymentCreatedQueue() {
        return QueueBuilder.durable("notification.payment.created").build();
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable("notification.payment.success").build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable("notification.payment.failed").build();
    }

    @Bean
    public Queue paymentCancelledQueue() {
        return QueueBuilder.durable("notification.payment.cancelled").build();
    }

    @Bean
    public Queue paymentRefundedQueue() {
        return QueueBuilder.durable("notification.payment.refunded").build();
    }

    @Bean
    public Binding paymentCreatedBinding(@Qualifier("paymentCreatedQueue") Queue queue,
                                         @Qualifier("ecommerceExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(PAYMENT_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentSuccessBinding(@Qualifier("paymentSuccessQueue") Queue queue,
                                         @Qualifier("ecommerceExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedBinding(@Qualifier("paymentFailedQueue") Queue queue,
                                        @Qualifier("ecommerceExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentCancelledBinding(@Qualifier("paymentCancelledQueue") Queue queue,
                                           @Qualifier("ecommerceExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(PAYMENT_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentRefundedBinding(@Qualifier("paymentRefundedQueue") Queue queue,
                                          @Qualifier("ecommerceExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(PAYMENT_REFUNDED_ROUTING_KEY);
    }
}
