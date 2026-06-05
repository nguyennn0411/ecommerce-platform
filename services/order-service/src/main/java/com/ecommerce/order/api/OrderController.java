package com.ecommerce.order.api;

import com.ecommerce.common.web.ApiResponse;
import com.ecommerce.order.api.dto.CancelOrderRequest;
import com.ecommerce.order.api.dto.CompletePaymentRequest;
import com.ecommerce.order.api.dto.CreateOrderRequest;
import com.ecommerce.order.api.dto.OrderResponse;
import com.ecommerce.order.api.dto.SagaTransactionResponse;
import com.ecommerce.order.api.dto.UpdateOrderStatusRequest;
import com.ecommerce.order.application.OrderUseCase;
import com.ecommerce.order.domain.OrderStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderUseCase orderUseCase;

    public OrderController(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderUseCase.createOrder(request));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> searchOrders(
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "status", required = false) OrderStatus status
    ) {
        return ApiResponse.ok(orderUseCase.searchOrders(userId, status));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable("orderId") UUID orderId) {
        return ApiResponse.ok(orderUseCase.getOrder(orderId));
    }

    @GetMapping("/code/{orderCode}")
    public ApiResponse<OrderResponse> getOrderByCode(@PathVariable("orderCode") String orderCode) {
        return ApiResponse.ok(orderUseCase.getOrderByCode(orderCode));
    }

    @PatchMapping("/{orderId}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ApiResponse.ok(orderUseCase.updateOrderStatus(orderId, request));
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        return ApiResponse.ok(orderUseCase.cancelOrder(orderId, request));
    }

    @PostMapping("/{orderId}/payment-completed")
    public ApiResponse<OrderResponse> completePayment(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody CompletePaymentRequest request
    ) {
        return ApiResponse.ok(orderUseCase.completePayment(orderId, request));
    }

    @GetMapping("/{orderId}/saga")
    public ApiResponse<List<SagaTransactionResponse>> getSagaTransactions(@PathVariable("orderId") UUID orderId) {
        return ApiResponse.ok(orderUseCase.getSagaTransactions(orderId));
    }
}
