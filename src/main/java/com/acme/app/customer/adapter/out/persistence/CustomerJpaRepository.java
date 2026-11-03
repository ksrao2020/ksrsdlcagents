package com.acme.app.customer.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {

    boolean existsByEmail(String email);
}
