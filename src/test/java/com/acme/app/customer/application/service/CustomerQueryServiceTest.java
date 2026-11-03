package com.acme.app.customer.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.acme.app.customer.application.port.out.CustomerRepositoryPort;
import com.acme.app.customer.domain.exception.CustomerNotFoundException;
import com.acme.app.customer.domain.model.Customer;
import com.acme.app.customer.domain.model.CustomerStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerQueryServiceTest {

    @Mock
    private CustomerRepositoryPort customerRepository;

    @InjectMocks
    private CustomerQueryService service;

    @Test
    void returnsExistingCustomer() {
        Customer customer = Customer.builder().id(1L).name("Ada").email("ada@example.com")
                .status(CustomerStatus.ACTIVE).build();
        given(customerRepository.findById(1L)).willReturn(Optional.of(customer));

        assertThat(service.getById(1L)).isEqualTo(customer);
    }

    @Test
    void throwsNotFoundForUnknownId() {
        given(customerRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CUSTOMER_NOT_FOUND");
    }
}
