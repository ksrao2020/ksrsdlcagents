package com.acme.app.customer.adapter.out.persistence;

import com.acme.app.customer.application.port.out.CustomerRepositoryPort;
import com.acme.app.customer.domain.model.Customer;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Implements the persistence port. Runs inside the transaction opened by the calling application service. */
@Component
@RequiredArgsConstructor
class CustomerPersistenceAdapter implements CustomerRepositoryPort {

    private final CustomerJpaRepository customerJpaRepository;
    private final CustomerPersistenceMapper customerPersistenceMapper;

    @Override
    public Customer save(Customer customer) {
        CustomerEntity saved = customerJpaRepository.save(customerPersistenceMapper.toEntity(customer));
        return customerPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Customer> findById(long customerId) {
        return customerJpaRepository.findById(customerId).map(customerPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return customerJpaRepository.existsByEmail(email);
    }
}
