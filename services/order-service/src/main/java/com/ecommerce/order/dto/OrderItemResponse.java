package com.ecommerce.order.dto;

import com.ecommerce.order.domain.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String size,
        String color,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    // Map entity OrderItem sang response để FE hiển thị từng sản phẩm trong đơn.
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getSize(),
                item.getColor(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}
