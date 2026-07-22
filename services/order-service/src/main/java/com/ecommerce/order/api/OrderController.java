package com.ecommerce.order.api;

import com.ecommerce.common.web.ApiResponse;
import com.ecommerce.order.application.OrderService;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.CancelOrderRequest;
import com.ecommerce.order.dto.SagaTransactionLogResponse;
import com.ecommerce.order.domain.OrderStatus;
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

    // FE gửi giỏ hàng và thông tin giao hàng vào đây để bắt đầu Saga.
    @PostMapping
    public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return ok(orderService.createOrder(request), "Order created and payment initiated");
    }

    // FE polling endpoint này sau khi thanh toán để đọc trạng thái mới nhất.
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getById(@PathVariable("orderId") UUID orderId) {
        return ok(orderService.getOrder(orderId), "OK");
    }

    // Trang My Orders dùng endpoint này để lấy lịch sử theo user.
    @GetMapping
    public ApiResponse<List<OrderResponse>> getByUser(@RequestParam("userId") UUID userId) {
        return ok(orderService.getOrdersByUser(userId), "OK");
    }

    // Customer chỉ được hủy chính đơn của mình khi PayOS vẫn đang chờ thanh toán.
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancel(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        return ok(orderService.cancelOrder(orderId, request.userId(), request.reason()), "Order cancelled");
    }

    // Staff dùng endpoint này để xem toàn bộ đơn, có thể lọc theo trạng thái.
    @GetMapping("/admin")
    public ApiResponse<List<OrderResponse>> getAll(@RequestParam(value = "status", required = false) OrderStatus status) {
        return ok(orderService.getAllOrders(status), "OK");
    }

    // Màn quản lý đơn dùng log này để hiển thị từng bước Saga và compensation.
    @GetMapping("/{orderId}/saga-logs")
    public ApiResponse<List<SagaTransactionLogResponse>> getSagaLogs(@PathVariable("orderId") UUID orderId) {
        return ok(orderService.getSagaLogs(orderId), "OK");
    }

    @PostMapping("/admin/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelByStaff(
            @PathVariable("orderId") UUID orderId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        return ok(orderService.cancelOrderByStaff(orderId, reason), "Order cancelled by staff");
    }

    // Staff chốt đơn đã thanh toán sau khi đối soát tiền cuối ngày.
    @PostMapping("/admin/{orderId}/complete")
    public ApiResponse<OrderResponse> completeByStaff(@PathVariable("orderId") UUID orderId) {
        return ok(orderService.completeOrderByStaff(orderId), "Order completed by staff");
    }

    // Staff chuyển đơn đã thanh toán sang trạng thái đang giao.
    @PostMapping("/admin/{orderId}/ship")
    public ApiResponse<OrderResponse> shipByStaff(@PathVariable("orderId") UUID orderId) {
        return ok(orderService.markShippingByStaff(orderId), "Order moved to shipping");
    }

    // Staff đánh dấu đơn đang giao bị hoàn về cửa hàng.
    @PostMapping("/admin/{orderId}/return")
    public ApiResponse<OrderResponse> returnByStaff(
            @PathVariable("orderId") UUID orderId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        return ok(orderService.returnOrderByStaff(orderId, reason), "Order returned by staff");
    }

    // Tất cả endpoint trả về cùng một response wrapper để FE xử lý đồng nhất.
    private <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now());
    }
}
