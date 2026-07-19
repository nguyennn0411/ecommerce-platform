package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.enums.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Re-checks pending links so a transient webhook delivery failure is recoverable. */
@Component
public class PayosPaymentReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PayosPaymentReconciliationScheduler.class);

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public PayosPaymentReconciliationScheduler(PaymentRepository paymentRepository, PaymentService paymentService) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    @Scheduled(fixedDelayString = "${payos.reconciliation-fixed-delay-ms:30000}", initialDelayString = "${payos.reconciliation-initial-delay-ms:15000}")
    public void reconcilePendingPayments() {
        for (Payment payment : paymentRepository.findTop50ByStatusOrderByCreatedAtAsc(PaymentStatus.PENDING)) {
            try {
                paymentService.syncPaymentByOrderId(payment.getOrderId());
            } catch (Exception exception) {
                log.debug("Unable to reconcile pending PayOS payment for order {}: {}",
                        payment.getOrderId(), exception.getMessage());
            }
        }
    }
}
