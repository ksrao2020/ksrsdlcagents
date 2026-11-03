package com.acme.app.shared.domain.exception;

/** Mapped to 422 UNPROCESSABLE ENTITY: the request is well-formed but breaks a domain invariant. */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
