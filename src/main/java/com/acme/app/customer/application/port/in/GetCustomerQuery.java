package com.acme.app.customer.application.port.in;

import com.acme.app.customer.domain.model.Customer;

public interface GetCustomerQuery {

    Customer getById(long customerId);
}
