package com.acme.app.customer.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.acme.app.customer.domain.model.Customer;
import com.acme.app.customer.domain.model.CustomerStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerPersistenceAdapterTest {

    @Mock
    private CustomerJpaRepository jpaRepository;

    private CustomerPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CustomerPersistenceAdapter(jpaRepository, Mappers.getMapper(CustomerPersistenceMapper.class));
    }

    @Test
    void savesThroughEntityAndReturnsPersistedDomain() {
        given(jpaRepository.save(any())).willAnswer(inv -> {
            CustomerEntity entity = inv.getArgument(0);
            entity.setId(11L);
            entity.setVersion(0L);
            return entity;
        });

        Customer saved = adapter.save(Customer.builder().name("Ada").email("ada@example.com")
                .status(CustomerStatus.ACTIVE).build());

        assertThat(saved.getId()).isEqualTo(11L);
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    void findsById() {
        given(jpaRepository.findById(11L)).willReturn(Optional.of(CustomerEntity.builder().id(11L).name("Ada")
                .email("ada@example.com").status(CustomerStatus.ACTIVE).version(3L).build()));

        assertThat(adapter.findById(11L)).get().extracting(Customer::getVersion).isEqualTo(3L);
        assertThat(adapter.findById(12L)).isEmpty();
    }

    @Test
    void delegatesEmailExistenceCheck() {
        given(jpaRepository.existsByEmail("ada@example.com")).willReturn(true);

        assertThat(adapter.existsByEmail("ada@example.com")).isTrue();
    }
}
