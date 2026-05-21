package com.ecommerce.productcatalog.domain;

import java.util.UUID;

public record ProductCatalog(
        UUID id,
        String name
) {
}
