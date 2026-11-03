package com.acme.app.acceptance;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Arrays;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public class CustomerSteps {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RequestPostProcessor token;
    private ResultActions lastResponse;

    /** Scenarios commit real data, so each one starts from an empty table. */
    @Before
    public void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM CUSTOMERS");
    }

    @Given("I am authenticated with scope {string}")
    public void authenticatedWithScope(String scopes) {
        token = jwt().authorities(Arrays.stream(scopes.split(" "))
                .<GrantedAuthority>map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .toList());
    }

    @Given("a customer with email {string} exists")
    public void customerExists(String email) throws Exception {
        registerCustomer("Existing", email);
        lastResponse.andExpect(status().isCreated());
    }

    @When("I register a customer named {string} with email {string}")
    public void registerCustomer(String name, String email) throws Exception {
        lastResponse = mvc.perform(post("/api/v1/customers").with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", name, "email", email))));
    }

    @Then("the response status is {int}")
    public void responseStatusIs(int expected) throws Exception {
        lastResponse.andExpect(status().is(expected));
    }

    @And("the error code is {string}")
    public void errorCodeIs(String errorCode) throws Exception {
        lastResponse.andExpect(jsonPath("$.errorCode").value(errorCode));
    }

    @And("the customer can be retrieved with email {string} and status {string}")
    public void customerCanBeRetrieved(String email, String customerStatus) throws Exception {
        String location = lastResponse.andReturn().getResponse().getHeader("Location");
        mvc.perform(get(location).with(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value(customerStatus));
    }
}
