package com.acme.app.customer.adapter.in.web;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.app.customer.application.port.in.CreateCustomerUseCase;
import com.acme.app.customer.application.port.in.GetCustomerQuery;
import com.acme.app.customer.domain.exception.CustomerNotFoundException;
import com.acme.app.customer.domain.exception.DuplicateCustomerEmailException;
import com.acme.app.customer.domain.model.Customer;
import com.acme.app.customer.domain.model.CustomerStatus;
import com.acme.app.shared.config.SecurityConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, CustomerWebMapperImpl.class})
class CustomerControllerTest {

    private static final String VALID_BODY = """
            {"name":"Ada","email":"ada@example.com"}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CreateCustomerUseCase createCustomerUseCase;

    @MockitoBean
    private GetCustomerQuery getCustomerQuery;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final Customer ADA = Customer.builder().id(5L).name("Ada").email("ada@example.com")
            .status(CustomerStatus.ACTIVE).version(0L).build();

    private static MockHttpServletRequestBuilder createRequest(String body) {
        return post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor scope(String scope) {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + scope));
    }

    @Nested
    class CreateCustomer {

        @Test
        void returns201WithLocation() throws Exception {
            given(createCustomerUseCase.create(any())).willReturn(ADA);

            mvc.perform(createRequest(VALID_BODY).with(scope("customers.write")))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", endsWith("/api/v1/customers/5")))
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void returns401WithoutToken() throws Exception {
            mvc.perform(createRequest(VALID_BODY)).andExpect(status().isUnauthorized());
        }

        @Test
        void returns403WithWrongScope() throws Exception {
            mvc.perform(createRequest(VALID_BODY).with(scope("customers.read")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }

        @Test
        void returns400WithViolationsForInvalidBody() throws Exception {
            mvc.perform(createRequest("""
                            {"name":"","email":"not-an-email"}""").with(scope("customers.write")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.violations.length()").value(2));
        }

        @Test
        void returns400ForMalformedJson() throws Exception {
            mvc.perform(createRequest("{oops").with(scope("customers.write")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
        }

        @Test
        void returns409ForDuplicateEmail() throws Exception {
            given(createCustomerUseCase.create(any())).willThrow(new DuplicateCustomerEmailException());

            mvc.perform(createRequest(VALID_BODY).with(scope("customers.write")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("CUSTOMER_EMAIL_ALREADY_EXISTS"));
        }
    }

    @Nested
    class GetCustomer {

        @Test
        void returns200() throws Exception {
            given(getCustomerQuery.getById(5L)).willReturn(ADA);

            mvc.perform(get("/api/v1/customers/5").with(scope("customers.read")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("ada@example.com"));
        }

        @Test
        void returns404ProblemForUnknownCustomer() throws Exception {
            given(getCustomerQuery.getById(9L)).willThrow(new CustomerNotFoundException(9L));

            mvc.perform(get("/api/v1/customers/9").with(scope("customers.read")))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                    .andExpect(jsonPath("$.errorCode").value("CUSTOMER_NOT_FOUND"));
        }

        @Test
        void returns400ForNonPositiveId() throws Exception {
            mvc.perform(get("/api/v1/customers/0").with(scope("customers.read")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        }

        @Test
        void returns403WithWrongScope() throws Exception {
            mvc.perform(get("/api/v1/customers/5").with(scope("customers.write")))
                    .andExpect(status().isForbidden());
        }
    }
}
