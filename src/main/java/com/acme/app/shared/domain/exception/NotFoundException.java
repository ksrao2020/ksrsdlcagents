package com.acme.app.shared.domain.exception;

/** Mapped to 404 NOT FOUND. */
public abstract class NotFoundException extends DomainException {

    protected NotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
