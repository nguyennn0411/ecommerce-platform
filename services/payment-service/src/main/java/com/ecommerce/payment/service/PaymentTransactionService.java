package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.enums.PaymentStatus;
import com.ecommerce.payment.enums.TransactionType;

public interface PaymentTransactionService {

    void record(Payment payment,
                TransactionType transactionType,
                PaymentStatus status,
                String providerReference,
                String providerResponse);
}
