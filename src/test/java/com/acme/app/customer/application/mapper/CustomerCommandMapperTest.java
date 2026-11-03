package com.acme.app.customer.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.app.customer.application.dto.CreateCustomerCommand;
import com.acme.app.customer.domain.model.Customer;
import com.acme.app.customer.domain.model.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CustomerCommandMapperTest {

    private final CustomerCommandMapper mapper = Mappers.getMapper(CustomerCommandMapper.class);

    @Test
    void mapsCommandToNewActiveCustomerWithoutIdOrVersion() {
        Customer customer = mapper.toNewCustomer(
                CreateCustomerCommand.builder().name("Ada").email("ada@example.com").build());

        assertThat(customer.getId()).isNull();
        assertThat(customer.getVersion()).isNull();
        assertThat(customer.getName()).isEqualTo("Ada");
        assertThat(customer.getEmail()).isEqualTo("ada@example.com");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void mapsNullToNull() {
        assertThat(mapper.toNewCustomer(null)).isNull();
    }
}
