package com.facturas.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response DTO for the /auth/me endpoint.
 * Returns the authenticated user's username extracted from the HttpOnly JWT cookie.
 */
@Getter
@AllArgsConstructor
public class UserInfoDTO {
    private final String username;
}
