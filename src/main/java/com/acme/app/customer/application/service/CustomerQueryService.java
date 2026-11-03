package com.acme.app.customer.application.service;

import com.acme.app.customer.application.port.in.GetCustomerQuery;
import com.acme.app.customer.application.port.out.CustomerRepositoryPort;
import com.acme.app.customer.domain.exception.CustomerNotFoundException;
import com.acme.app.customer.domain.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side: read-only transactions skip Hibernate dirty checking and flushes. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerQueryService implements GetCustomerQuery {

    private final CustomerRepositoryPort customerRepository;

    @Override
    public Customer getById(long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
