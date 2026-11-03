package com.acme.app.customer.domain.model;

import com.acme.app.shared.domain.exception.BusinessRuleViolationException;
import java.util.Locale;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Customer aggregate root. Immutable: behaviour methods return a new instance via {@code toBuilder()}.
 *
 * <p>{@code @Builder} sits on the constructor so that every creation path (MapStruct, {@code toBuilder()},
 * rehydration from the database) goes through the invariant checks below.
 */
@Getter
@ToString
@EqualsAndHashCode
public class Customer {

    private final Long id;
    private final String name;
    private final String email;
    private final CustomerStatus status;
    private final Long version;

    @Builder(toBuilder = true)
    private Customer(Long id, String name, String email, CustomerStatus status, Long version) {
        require(name != null && !name.isBlank(), "CUSTOMER_NAME_REQUIRED", "Customer name is required");
        require(email != null && email.contains("@"), "CUSTOMER_EMAIL_INVALID", "Customer email is invalid");
        require(status != null, "CUSTOMER_STATUS_REQUIRED", "Customer status is required");
        this.id = id;
        this.name = name.strip();
        this.email = email.strip().toLowerCase(Locale.ROOT);
        this.status = status;
        this.version = version;
    }

    public Customer suspend() {
        require(status == CustomerStatus.ACTIVE, "CUSTOMER_ALREADY_SUSPENDED", "Customer is already suspended");
        return toBuilder().status(CustomerStatus.SUSPENDED).build();
    }

    private static void require(boolean condition, String errorCode, String message) {
        if (!condition) {
            throw new BusinessRuleViolationException(errorCode, message);
        }
    }
}
