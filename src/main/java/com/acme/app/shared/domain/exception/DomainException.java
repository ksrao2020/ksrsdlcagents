package com.acme.app.shared.domain.exception;

import lombok.Getter;

/**
 * Base type for every business exception. The {@code errorCode} is a stable, machine-readable value that
 * the global exception handler returns to API clients, so never change an existing code.
 */
@Getter
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
