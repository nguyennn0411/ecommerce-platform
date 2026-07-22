package com.ecommerce.order.exception;

import com.ecommerce.common.web.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ecommerce.order")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrderGlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleOrderNotFound(OrderNotFoundException exception) {
        return ApiResponse.error(exception.getMessage());
    }

    @ExceptionHandler(OrderIntegrationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiResponse<Void> handleOrderIntegration(OrderIntegrationException exception) {
        return ApiResponse.error(exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleInvalidOrderState(IllegalStateException exception) {
        return ApiResponse.error(exception.getMessage());
    }
}
