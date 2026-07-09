package com.ecommerce.user.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInternalDTO {
    private Integer id;
    private String email;
    private String role;
    private String status;
    private boolean emailVerified;
}
