package com.securevault.vault.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class RequestContext {

    private RequestContext() {}

    public static UUID getUserId(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("X-User-Id header is missing");
        }
        return UUID.fromString(header);
    }

    public static String getUserRole(HttpServletRequest request) {
        return request.getHeader("X-User-Role");
    }

    public static boolean isAdmin(HttpServletRequest request) {
        return "ADMIN".equals(getUserRole(request));
    }
}
