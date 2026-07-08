package com.ecommerce.inventory.exception;

import com.ecommerce.common.web.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ecommerce.inventory")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InventoryGlobalExceptionHandler {

    @ExceptionHandler(InventoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(InventoryNotFoundException exception) {
        return ApiResponse.error(exception.getMessage());
    }
}
