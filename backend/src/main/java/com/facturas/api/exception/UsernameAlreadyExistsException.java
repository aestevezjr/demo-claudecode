package com.facturas.api.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
        super("El nombre de usuario '" + username + "' ya está en uso");
    }
}
