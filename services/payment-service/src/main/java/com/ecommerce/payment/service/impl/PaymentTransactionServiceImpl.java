package com.ecommerce.payment.service.impl;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentTransaction;
import com.ecommerce.payment.enums.PaymentStatus;
import com.ecommerce.payment.enums.TransactionType;
import com.ecommerce.payment.repository.PaymentTransactionRepository;
import com.ecommerce.payment.service.PaymentTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentTransactionServiceImpl implements PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentTransactionServiceImpl(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Override
    @Transactional
    public void record(Payment payment,
                       TransactionType transactionType,
                       PaymentStatus status,
                       String providerReference,
                       String providerResponse) {
        paymentTransactionRepository.save(new PaymentTransaction(
                payment,
                transactionType,
                status,
                providerReference,
                providerResponse
        ));
    }
}
