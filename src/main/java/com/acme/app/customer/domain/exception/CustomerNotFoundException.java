package com.acme.app.customer.domain.exception;

import com.acme.app.shared.domain.exception.NotFoundException;

public class CustomerNotFoundException extends NotFoundException {

    public CustomerNotFoundException(long customerId) {
        super("CUSTOMER_NOT_FOUND", "Customer " + customerId + " was not found");
    }
}
