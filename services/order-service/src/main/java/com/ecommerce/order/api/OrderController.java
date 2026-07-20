package com.ecommerce.order.api;

import com.ecommerce.common.web.ApiResponse;
import com.ecommerce.order.application.OrderService;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // FE gui gio hang va thong tin giao hang vao day de bat dau Saga.
    @PostMapping
    public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return ok(orderService.createOrder(request), "Order created and payment initiated");
    }

    // FE polling endpoint nay sau khi thanh toan de doc trang thai moi nhat.
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getById(@PathVariable("orderId") UUID orderId) {
        return ok(orderService.getOrder(orderId), "OK");
    }

    // Trang My Orders dung endpoint nay de lay lich su theo user.
    @GetMapping
    public ApiResponse<List<OrderResponse>> getByUser(@RequestParam("userId") UUID userId) {
        return ok(orderService.getOrdersByUser(userId), "OK");
    }

    // Tat ca endpoint tra ve cung mot response wrapper de FE xu ly dong nhat.
    private <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now());
    }
}
