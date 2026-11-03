package com.acme.app.customer.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/** Input of the "create customer" use case. Validated by the {@code @Validated} application service. */
@Builder
public record CreateCustomerCommand(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 254) String email) {
}
