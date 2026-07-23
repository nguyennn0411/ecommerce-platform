package com.ecommerce.order.messaging;

import com.ecommerce.common.events.PaymentCancelledEvent;
import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentSuccessEvent;
import com.ecommerce.order.application.OrderService;
import com.ecommerce.order.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final OrderService orderService;

    public PaymentEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    // Nhận event payment.success từ Payment Service để xác nhận đơn đã thanh toán.
    @RabbitListener(queues = RabbitMqConfig.ORDER_PAYMENT_SUCCESS_QUEUE)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        // Nhận payment.success -> Order xác nhận đơn và trừ kho thật.
        handle("payment.success", () -> orderService.handlePaymentSuccess(event.orderId()));
    }

    // Nhận event payment.failed từ Payment Service để chuyển đơn sang lỗi và trả hàng.
    @RabbitListener(queues = RabbitMqConfig.ORDER_PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(PaymentFailedEvent event) {
        // Payment fail -> trả hàng, đơn FAILED.
        handle("payment.failed", () -> orderService.handlePaymentFailure(event.orderId(), event.reason(), false));
    }

    // Nhận event payment.cancelled từ Payment Service khi người dùng hủy thanh toán PayOS.
    @RabbitListener(queues = RabbitMqConfig.ORDER_PAYMENT_CANCELLED_QUEUE)
    public void onPaymentCancelled(PaymentCancelledEvent event) {
        // Người dùng hủy PayOS -> trả hàng, đơn CANCELLED.
        handle("payment.cancelled", () -> orderService.handlePaymentFailure(event.orderId(), event.reason(), true));
    }

    // Bọc xử lý event để listener không chết khi một message gặp lỗi nghiệp vụ.
    private void handle(String eventName, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            // Log để vận hành kiểm tra và xử lý lại event lỗi.
            log.error("Unable to process {}", eventName, exception);
        }
    }
}
