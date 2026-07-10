package com.ecommerce.productcatalog.api;

import com.ecommerce.common.web.ApiResponse;
import com.ecommerce.productcatalog.api.dto.CategoryResponse;
import com.ecommerce.productcatalog.api.dto.CreateCategoryRequest;
import com.ecommerce.productcatalog.api.dto.CreateProductRequest;
import com.ecommerce.productcatalog.api.dto.ProductResponse;
import com.ecommerce.productcatalog.api.dto.ProductValidationResponse;
import com.ecommerce.productcatalog.api.dto.UpdateProductRequest;
import com.ecommerce.productcatalog.api.dto.ValidateProductsRequest;
import com.ecommerce.productcatalog.application.ProductCatalogUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductCatalogUseCase productCatalogUseCase;

    public ProductController(ProductCatalogUseCase productCatalogUseCase) {
        this.productCatalogUseCase = productCatalogUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.ok(productCatalogUseCase.createProduct(request), "Product created");
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> searchProducts(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.ok(productCatalogUseCase.searchProducts(keyword, categoryId, status));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable("productId") UUID productId) {
        return ApiResponse.ok(productCatalogUseCase.getProduct(productId));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable("productId") UUID productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ApiResponse.ok(productCatalogUseCase.updateProduct(productId, request), "Product updated");
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<ProductResponse> discontinueProduct(@PathVariable("productId") UUID productId) {
        return ApiResponse.ok(productCatalogUseCase.discontinueProduct(productId), "Product discontinued");
    }

    /**
     * Order-service Feign contract — returns raw body (not ApiResponse wrapper).
     */
    @PostMapping("/validation")
    public ProductValidationResponse validateProducts(@RequestBody ValidateProductsRequest request) {
        return productCatalogUseCase.validateProducts(request);
    }
}
