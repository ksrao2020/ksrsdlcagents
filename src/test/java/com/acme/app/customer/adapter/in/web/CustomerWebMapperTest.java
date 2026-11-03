package com.acme.app.customer.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.app.api.model.CreateCustomerRequest;
import com.acme.app.api.model.CustomerResponse;
import com.acme.app.customer.application.dto.CreateCustomerCommand;
import com.acme.app.customer.domain.model.Customer;
import com.acme.app.customer.domain.model.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CustomerWebMapperTest {

    private final CustomerWebMapper mapper = Mappers.getMapper(CustomerWebMapper.class);

    @Test
    void mapsRequestToCommand() {
        CreateCustomerCommand command = mapper.toCommand(new CreateCustomerRequest("Ada", "ada@example.com"));

        assertThat(command).isEqualTo(new CreateCustomerCommand("Ada", "ada@example.com"));
    }

    @Test
    void mapsDomainToResponse() {
        Customer customer = Customer.builder().id(3L).name("Ada").email("ada@example.com")
                .status(CustomerStatus.SUSPENDED).version(2L).build();

        CustomerResponse response = mapper.toResponse(customer);

        assertThat(response).isEqualTo(
                new CustomerResponse(3L, "Ada", "ada@example.com", CustomerResponse.StatusEnum.SUSPENDED));
    }

    @Test
    void mapsNullToNull() {
        assertThat(mapper.toCommand(null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
    }
}
