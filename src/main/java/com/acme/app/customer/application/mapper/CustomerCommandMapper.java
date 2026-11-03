package com.acme.app.customer.application.mapper;

import com.acme.app.customer.application.dto.CreateCustomerCommand;
import com.acme.app.customer.domain.model.Customer;
import com.acme.app.shared.mapping.CentralMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Command DTO → domain model. MapStruct builds the aggregate through its Lombok builder. */
@Mapper(config = CentralMapperConfig.class)
public interface CustomerCommandMapper {

    @Mapping(target = "id", ignore = true)                 // assigned by the database sequence
    @Mapping(target = "version", ignore = true)            // managed by JPA optimistic locking
    @Mapping(target = "status", constant = "ACTIVE")       // every new customer starts active
    Customer toNewCustomer(CreateCustomerCommand command);
}
