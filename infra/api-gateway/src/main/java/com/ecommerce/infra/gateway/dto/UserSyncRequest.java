package com.ecommerce.infra.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSyncRequest {
    private String email;
}