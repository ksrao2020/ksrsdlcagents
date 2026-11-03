package com.acme.app.shared.domain.exception;

/** Mapped to 409 CONFLICT, e.g. a uniqueness rule that the client can resolve by changing its request. */
public abstract class ConflictException extends DomainException {

    protected ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
