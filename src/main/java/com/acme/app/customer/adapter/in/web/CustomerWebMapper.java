package com.acme.app.customer.adapter.in.web;

import com.acme.app.api.model.CreateCustomerRequest;
import com.acme.app.api.model.CustomerResponse;
import com.acme.app.customer.application.dto.CreateCustomerCommand;
import com.acme.app.customer.domain.model.Customer;
import com.acme.app.shared.mapping.CentralMapperConfig;
import org.mapstruct.Mapper;

/** API request → command DTO, and domain model → API response. Generated API models stop here. */
@Mapper(config = CentralMapperConfig.class)
public interface CustomerWebMapper {

    CreateCustomerCommand toCommand(CreateCustomerRequest request);

    CustomerResponse toResponse(Customer customer);
}
