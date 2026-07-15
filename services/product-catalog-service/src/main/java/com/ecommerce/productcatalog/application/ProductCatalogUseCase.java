package com.ecommerce.productcatalog.application;

import com.ecommerce.productcatalog.api.dto.CategoryResponse;
import com.ecommerce.productcatalog.api.dto.CreateCategoryRequest;
import com.ecommerce.productcatalog.api.dto.CreateProductRequest;
import com.ecommerce.productcatalog.api.dto.ProductImageRequest;
import com.ecommerce.productcatalog.api.dto.ProductImageResponse;
import com.ecommerce.productcatalog.api.dto.ProductResponse;
import com.ecommerce.productcatalog.api.dto.ProductValidationResponse;
import com.ecommerce.productcatalog.api.dto.ProductVariantRequest;
import com.ecommerce.productcatalog.api.dto.ProductVariantResponse;
import com.ecommerce.productcatalog.api.dto.UpdateProductRequest;
import com.ecommerce.productcatalog.api.dto.ValidateProductItemRequest;
import com.ecommerce.productcatalog.api.dto.ValidateProductsRequest;
import com.ecommerce.productcatalog.domain.Category;
import com.ecommerce.productcatalog.domain.Product;
import com.ecommerce.productcatalog.domain.ProductImage;
import com.ecommerce.productcatalog.domain.ProductStatus;
import com.ecommerce.productcatalog.domain.ProductVariant;
import com.ecommerce.productcatalog.persistence.CategoryRepository;
import com.ecommerce.productcatalog.persistence.ProductRepository;
import com.ecommerce.productcatalog.persistence.ProductSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProductCatalogUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductCatalogUseCase(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        Category category = new Category();
        category.setName(request.name().trim());
        category.setDescription(trimToNull(request.description()));
        return toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = new Product();
        applyProductFields(
                product,
                request.name(),
                request.brand(),
                request.description(),
                request.categoryId(),
                request.basePrice(),
                parseStatus(request.status(), ProductStatus.ACTIVE)
        );
        replaceVariants(product, request.variants());
        replaceImages(product, request.images());
        Product saved = productRepository.save(product);
        return toProductResponse(loadProduct(saved.getId()));
    }

    @Transactional
    public ProductResponse updateProduct(UUID productId, UpdateProductRequest request) {
        Product product = loadProduct(productId);
        applyProductFields(
                product,
                request.name(),
                request.brand(),
                request.description(),
                request.categoryId(),
                request.basePrice(),
                parseStatus(request.status(), null)
        );
        replaceVariants(product, request.variants());
        replaceImages(product, request.images());
        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse discontinueProduct(UUID productId) {
        Product product = loadProduct(productId);
        product.setStatus(ProductStatus.DISCONTINUED);
        return toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId) {
        return toProductResponse(loadProduct(productId));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword, UUID categoryId, String status) {
        String normalizedKeyword = normalizeKeyword(keyword);
        ProductStatus productStatus = status == null || status.isBlank() ? null : parseStatus(status, null);
        return productRepository
                .findAll(
                        ProductSpecifications.withFilters(normalizedKeyword, categoryId, productStatus),
                        Sort.by(Sort.Direction.ASC, "name")
                )
                .stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductValidationResponse validateProducts(ValidateProductsRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            return new ProductValidationResponse(false, "Validation request must contain at least one item");
        }

        for (ValidateProductItemRequest item : request.items()) {
            ProductValidationResponse itemResult = validateItem(item);
            if (!itemResult.valid()) {
                return itemResult;
            }
        }
        return new ProductValidationResponse(true, "All products are valid");
    }

    private ProductValidationResponse validateItem(ValidateProductItemRequest item) {
        if (item == null || item.productId() == null) {
            return new ProductValidationResponse(false, "productId is required");
        }
        if (item.quantity() == null || item.quantity() < 1) {
            return new ProductValidationResponse(false, "quantity must be >= 1 for productId=" + item.productId());
        }
        if (item.unitPrice() == null || item.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return new ProductValidationResponse(false, "unitPrice must be > 0 for productId=" + item.productId());
        }

        Product product = productRepository.findById(item.productId()).orElse(null);
        if (product == null) {
            return new ProductValidationResponse(false, "Product not found: " + item.productId());
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            return new ProductValidationResponse(
                    false,
                    "Product is not ACTIVE: " + item.productId() + " status=" + product.getStatus()
            );
        }
        if (product.getBasePrice().compareTo(item.unitPrice()) != 0) {
            return new ProductValidationResponse(
                    false,
                    "Price mismatch for productId=%s expected=%s actual=%s"
                            .formatted(item.productId(), product.getBasePrice(), item.unitPrice())
            );
        }
        return new ProductValidationResponse(true, "OK");
    }

    private Product loadProduct(UUID productId) {
        return productRepository.findWithCategoryById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    private void applyProductFields(
            Product product,
            String name,
            String brand,
            String description,
            UUID categoryId,
            BigDecimal basePrice,
            ProductStatus status
    ) {
        product.setName(name.trim());
        product.setBrand(trimToNull(brand));
        product.setDescription(trimToNull(description));
        product.setBasePrice(basePrice);
        product.setStatus(status);
        if (categoryId == null) {
            product.setCategory(null);
        } else {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
            product.setCategory(category);
        }
    }

    private void replaceVariants(Product product, List<ProductVariantRequest> requests) {
        product.clearVariants();
        if (requests == null) {
            return;
        }
        for (ProductVariantRequest request : requests) {
            ProductVariant variant = new ProductVariant();
            variant.setSize(request.size().trim());
            variant.setColor(normalizeColor(request.color()));
            variant.setSku(trimToNull(request.sku()));
            product.addVariant(variant);
        }
    }

    private void replaceImages(Product product, List<ProductImageRequest> requests) {
        product.clearImages();
        if (requests == null) {
            return;
        }
        for (ProductImageRequest request : requests) {
            ProductImage image = new ProductImage();
            image.setImageUrl(request.imageUrl().trim());
            image.setMain(Boolean.TRUE.equals(request.main()));
            product.addImage(image);
        }
    }

    private ProductResponse toProductResponse(Product product) {
        // Touch lazy collections inside the transaction.
        List<ProductVariantResponse> variants = product.getVariants().stream()
                .map(variant -> new ProductVariantResponse(
                        variant.getId(),
                        variant.getSize(),
                        variant.getColor(),
                        variant.getSku()
                ))
                .toList();
        List<ProductImageResponse> images = product.getImages().stream()
                .map(image -> new ProductImageResponse(image.getId(), image.getImageUrl(), image.isMain()))
                .toList();

        Category category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getDescription(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                product.getBasePrice(),
                product.getStatus().name(),
                variants,
                images,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private ProductStatus parseStatus(String status, ProductStatus defaultStatus) {
        if (status == null || status.isBlank()) {
            if (defaultStatus == null) {
                throw new IllegalArgumentException("status is required");
            }
            return defaultStatus;
        }
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        return color.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
