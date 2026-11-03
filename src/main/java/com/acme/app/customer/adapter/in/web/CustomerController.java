package com.acme.app.customer.adapter.in.web;

import com.acme.app.api.CustomersApi;
import com.acme.app.api.model.CreateCustomerRequest;
import com.acme.app.api.model.CustomerResponse;
import com.acme.app.customer.application.port.in.CreateCustomerUseCase;
import com.acme.app.customer.application.port.in.GetCustomerQuery;
import com.acme.app.customer.domain.model.Customer;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Implements the OpenAPI-generated {@link CustomersApi}: routes, request validation and media types come from
 * {@code api.yaml}. The controller only maps, delegates to a use case and secures the operation.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CustomerController implements CustomersApi {

    private final CreateCustomerUseCase createCustomerUseCase;
    private final GetCustomerQuery getCustomerQuery;
    private final CustomerWebMapper customerWebMapper;

    @Override
    @PreAuthorize("hasAuthority('SCOPE_customers.write')")
    public ResponseEntity<CustomerResponse> createCustomer(CreateCustomerRequest request) {
        Customer created = createCustomerUseCase.create(customerWebMapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(customerWebMapper.toResponse(created));
    }

    @Override
    @PreAuthorize("hasAuthority('SCOPE_customers.read')")
    public ResponseEntity<CustomerResponse> getCustomer(Long customerId) {
        return ResponseEntity.ok(customerWebMapper.toResponse(getCustomerQuery.getById(customerId)));
    }
}
