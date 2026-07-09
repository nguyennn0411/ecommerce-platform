package com.ecommerce.infra.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInternalDTO {
    private Integer id;
    private String email;
    private String role;
    private String status;
    private boolean emailVerified;
}
