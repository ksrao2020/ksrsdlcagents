package com.acme.app.customer.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.app.customer.domain.model.Customer;
import com.acme.app.customer.domain.model.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CustomerPersistenceMapperTest {

    private final CustomerPersistenceMapper mapper = Mappers.getMapper(CustomerPersistenceMapper.class);

    @Test
    void roundTripsAllFieldsIncludingVersion() {
        Customer customer = Customer.builder().id(4L).name("Ada").email("ada@example.com")
                .status(CustomerStatus.SUSPENDED).version(9L).build();

        CustomerEntity entity = mapper.toEntity(customer);

        assertThat(entity).usingRecursiveComparison().isEqualTo(customer);
        assertThat(mapper.toDomain(entity)).isEqualTo(customer);
    }

    @Test
    void mapsNullToNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
