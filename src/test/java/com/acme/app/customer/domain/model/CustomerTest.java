package com.acme.app.customer.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.app.shared.domain.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CustomerTest {

    private static Customer.CustomerBuilder validCustomer() {
        return Customer.builder().id(1L).name("Ada Lovelace").email("ada@example.com").status(CustomerStatus.ACTIVE);
    }

    @Test
    void normalisesNameAndEmail() {
        Customer customer = validCustomer().name("  Ada  ").email(" ADA@Example.COM ").build();

        assertThat(customer.getName()).isEqualTo("Ada");
        assertThat(customer.getEmail()).isEqualTo("ada@example.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectsMissingName(String name) {
        assertThatThrownBy(() -> validCustomer().name(name).build())
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CUSTOMER_NAME_REQUIRED");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "not-an-email")
    void rejectsInvalidEmail(String email) {
        assertThatThrownBy(() -> validCustomer().email(email).build())
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CUSTOMER_EMAIL_INVALID");
    }

    @Test
    void rejectsMissingStatus() {
        assertThatThrownBy(() -> validCustomer().status(null).build())
                .hasFieldOrPropertyWithValue("errorCode", "CUSTOMER_STATUS_REQUIRED");
    }

    @Test
    void suspendsAnActiveCustomerWithoutMutatingTheOriginal() {
        Customer active = validCustomer().build();

        Customer suspended = active.suspend();

        assertThat(suspended.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
        assertThat(active.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void cannotSuspendTwice() {
        Customer suspended = validCustomer().status(CustomerStatus.SUSPENDED).build();

        assertThatThrownBy(suspended::suspend)
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CUSTOMER_ALREADY_SUSPENDED");
    }
}
