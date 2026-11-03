package com.acme.app.customer.domain.exception;

import com.acme.app.shared.domain.exception.ConflictException;

public class DuplicateCustomerEmailException extends ConflictException {

    public DuplicateCustomerEmailException() {
        super("CUSTOMER_EMAIL_ALREADY_EXISTS", "A customer with this email already exists");
    }
}
