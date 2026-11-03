package com.acme.app.customer.application.service;

import com.acme.app.customer.application.dto.CreateCustomerCommand;
import com.acme.app.customer.application.mapper.CustomerCommandMapper;
import com.acme.app.customer.application.port.in.CreateCustomerUseCase;
import com.acme.app.customer.application.port.out.CustomerRepositoryPort;
import com.acme.app.customer.domain.exception.DuplicateCustomerEmailException;
import com.acme.app.customer.domain.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Write side: every method runs in a read-write transaction that rolls back on any runtime exception. */
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class CustomerCommandService implements CreateCustomerUseCase {

    private final CustomerRepositoryPort customerRepository;
    private final CustomerCommandMapper customerCommandMapper;

    @Override
    public Customer create(CreateCustomerCommand command) {
        Customer customer = customerCommandMapper.toNewCustomer(command);
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new DuplicateCustomerEmailException();
        }
        return customerRepository.save(customer);
    }
}
