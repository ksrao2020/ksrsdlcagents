package com.acme.app.customer.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.app.OracleTestcontainersConfig;
import com.acme.app.customer.domain.model.Customer;
import com.acme.app.customer.domain.model.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Runs against real Oracle (Flyway schema, sequence, constraints, optimistic locking). Needs Docker: mvn verify -Pit */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OracleTestcontainersConfig.class, CustomerPersistenceAdapter.class, CustomerPersistenceMapperImpl.class})
class CustomerPersistenceAdapterIT {

    @Autowired
    private CustomerPersistenceAdapter adapter;

    @Autowired
    private CustomerJpaRepository jpaRepository;

    private static Customer newCustomer(String email) {
        return Customer.builder().name("Ada").email(email).status(CustomerStatus.ACTIVE).build();
    }

    @Test
    void persistsAndReadsBack() {
        Customer saved = adapter.save(newCustomer("ada@example.com"));
        jpaRepository.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(adapter.findById(saved.getId())).contains(saved);
        assertThat(adapter.existsByEmail("ada@example.com")).isTrue();
    }

    @Test
    void uniqueEmailIsEnforcedByTheDatabase() {
        adapter.save(newCustomer("dup@example.com"));
        adapter.save(newCustomer("dup@example.com"));

        assertThatThrownBy(jpaRepository::flush).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleVersionIsRejected() {
        Customer saved = adapter.save(newCustomer("lock@example.com"));
        adapter.save(saved.suspend());

        Customer stale = saved.toBuilder().name("Stale write").build();
        assertThatThrownBy(() -> adapter.save(stale)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        jpaRepository.deleteAll();
    }
}
