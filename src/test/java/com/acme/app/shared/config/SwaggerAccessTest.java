package com.acme.app.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.app.customer.adapter.in.web.CustomerController;
import com.acme.app.customer.adapter.in.web.CustomerWebMapperImpl;
import com.acme.app.customer.application.port.in.CreateCustomerUseCase;
import com.acme.app.customer.application.port.in.GetCustomerQuery;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Swagger is open without a token only in the local profile. Springdoc is not in this slice, so open means not 401. */
class SwaggerAccessTest {

    abstract static class WebSlice {

        @Autowired
        protected MockMvc mvc;

        @MockitoBean
        private CreateCustomerUseCase createCustomerUseCase;

        @MockitoBean
        private GetCustomerQuery getCustomerQuery;

        @MockitoBean
        private JwtDecoder jwtDecoder;
    }

    @Nested
    @WebMvcTest(CustomerController.class)
    @ActiveProfiles("local")
    @Import({SecurityConfig.class, LocalSwaggerSecurityConfig.class, CustomerWebMapperImpl.class})
    class WithLocalProfile extends WebSlice {

        @ParameterizedTest
        @ValueSource(strings = {"/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/swagger-config"})
        void swaggerNeedsNoToken(String path) throws Exception {
            int responseStatus = mvc.perform(get(path)).andReturn().getResponse().getStatus();

            assertThat(responseStatus).isNotEqualTo(401);
        }

        @Test
        void apiStillNeedsAToken() throws Exception {
            mvc.perform(get("/api/v1/customers/1")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @WebMvcTest(CustomerController.class)
    @Import({SecurityConfig.class, LocalSwaggerSecurityConfig.class, CustomerWebMapperImpl.class})
    class WithoutLocalProfile extends WebSlice {

        @ParameterizedTest
        @ValueSource(strings = {"/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/swagger-config"})
        void swaggerStaysLocked(String path) throws Exception {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }
}
