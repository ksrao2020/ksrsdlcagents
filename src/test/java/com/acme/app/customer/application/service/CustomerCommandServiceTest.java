package com.acme.app.customer.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.acme.app.customer.application.dto.CreateCustomerCommand;
import com.acme.app.customer.application.mapper.CustomerCommandMapper;
import com.acme.app.customer.application.port.out.CustomerRepositoryPort;
import com.acme.app.customer.domain.exception.DuplicateCustomerEmailException;
import com.acme.app.customer.domain.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerCommandServiceTest {

    @Mock
    private CustomerRepositoryPort customerRepository;

    private CustomerCommandService service;

    private final CreateCustomerCommand command =
            CreateCustomerCommand.builder().name("Ada").email("ADA@example.com").build();

    @BeforeEach
    void setUp() {
        service = new CustomerCommandService(customerRepository, Mappers.getMapper(CustomerCommandMapper.class));
    }

    @Test
    void createsCustomerWhenEmailIsUnused() {
        given(customerRepository.existsByEmail("ada@example.com")).willReturn(false);
        given(customerRepository.save(any())).willAnswer(inv -> inv.<Customer>getArgument(0).toBuilder().id(7L).build());

        Customer created = service.create(command);

        assertThat(created.getId()).isEqualTo(7L);
        assertThat(created.getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void rejectsDuplicateEmail() {
        given(customerRepository.existsByEmail("ada@example.com")).willReturn(true);

        assertThatThrownBy(() -> service.create(command)).isInstanceOf(DuplicateCustomerEmailException.class);
        then(customerRepository).should(never()).save(any());
    }
}
