package com.acme.app.customer.application.port.in;

import com.acme.app.customer.application.dto.CreateCustomerCommand;
import com.acme.app.customer.domain.model.Customer;
import jakarta.validation.Valid;

public interface CreateCustomerUseCase {

    Customer create(@Valid CreateCustomerCommand command);
}
