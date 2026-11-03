package com.acme.app.acceptance;

import com.acme.app.OracleTestcontainersConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Full application context against real Oracle. Tokens are injected with MockMvc's jwt() post-processor. */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@Import(OracleTestcontainersConfig.class)
public class CucumberSpringConfiguration {

    @MockitoBean
    private JwtDecoder jwtDecoder;
}
