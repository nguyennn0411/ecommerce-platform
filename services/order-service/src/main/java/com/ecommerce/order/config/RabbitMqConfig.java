package com.ecommerce.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    public static final String ECOMMERCE_EXCHANGE = "ecommerce.exchange";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String PAYMENT_CANCELLED_ROUTING_KEY = "payment.cancelled";

    public static final String ORDER_PAYMENT_SUCCESS_QUEUE = "order.payment.success";
    public static final String ORDER_PAYMENT_FAILED_QUEUE = "order.payment.failed";
    public static final String ORDER_PAYMENT_CANCELLED_QUEUE = "order.payment.cancelled";

    @Bean
    public TopicExchange ecommerceExchange() {
        return new TopicExchange(ECOMMERCE_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
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
    public Queue orderPaymentSuccessQueue() {
        return QueueBuilder.durable(ORDER_PAYMENT_SUCCESS_QUEUE).build();
    }

    @Bean
    public Queue orderPaymentFailedQueue() {
        return QueueBuilder.durable(ORDER_PAYMENT_FAILED_QUEUE).build();
    }

    @Bean
    public Queue orderPaymentCancelledQueue() {
        return QueueBuilder.durable(ORDER_PAYMENT_CANCELLED_QUEUE).build();
    }

    @Bean
    public Binding orderPaymentSuccessBinding(@Qualifier("orderPaymentSuccessQueue") Queue queue,
                                              @Qualifier("ecommerceExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding orderPaymentFailedBinding(@Qualifier("orderPaymentFailedQueue") Queue queue,
                                             @Qualifier("ecommerceExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding orderPaymentCancelledBinding(@Qualifier("orderPaymentCancelledQueue") Queue queue,
                                                @Qualifier("ecommerceExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(PAYMENT_CANCELLED_ROUTING_KEY);
    }
}
