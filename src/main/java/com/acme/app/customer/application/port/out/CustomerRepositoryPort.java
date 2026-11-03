package com.acme.app.customer.application.port.out;

import com.acme.app.customer.domain.model.Customer;
import java.util.Optional;

/** Persistence port. Speaks only domain types; the JPA adapter implements it. */
public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findById(long customerId);

    boolean existsByEmail(String email);
}
