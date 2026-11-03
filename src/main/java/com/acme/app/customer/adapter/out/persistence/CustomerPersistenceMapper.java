package com.acme.app.customer.adapter.out.persistence;

import com.acme.app.customer.domain.model.Customer;
import com.acme.app.shared.mapping.CentralMapperConfig;
import org.mapstruct.Mapper;

/** Domain model ↔ JPA entity. Carries {@code version} both ways so optimistic locking works on updates. */
@Mapper(config = CentralMapperConfig.class)
public interface CustomerPersistenceMapper {

    CustomerEntity toEntity(Customer customer);

    Customer toDomain(CustomerEntity entity);
}
