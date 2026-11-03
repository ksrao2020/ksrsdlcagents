package com.acme.app;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.oracle.OracleContainer;

/** Real Oracle for integration and acceptance tests. Spring Boot wires the datasource via {@code @ServiceConnection}. */
@TestConfiguration(proxyBeanMethods = false)
public class OracleTestcontainersConfig {

    @Bean
    @ServiceConnection
    OracleContainer oracleContainer() {
        return new OracleContainer("gvenzl/oracle-free:23-slim-faststart").withReuse(true);
    }
}
