package com.acme.app.shared.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.app.shared.domain.exception.BusinessRuleViolationException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/** Covers the handler branches that the controller slice tests cannot easily trigger. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    record Sample(@NotBlank String name) {
    }

    @Test
    void businessRuleViolationIs422() {
        ProblemDetail problem = handler.handleBusinessRule(new BusinessRuleViolationException("RULE_X", "broken"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getProperties()).containsEntry("errorCode", "RULE_X");
    }

    @Test
    void constraintViolationIs400WithFieldNames() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(new Sample(""));

        ProblemDetail problem = handler.handleConstraintViolation(new ConstraintViolationException(violations));

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getProperties()).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat((List<?>) problem.getProperties().get("violations"))
                .singleElement().extracting("field").isEqualTo("name");
    }

    @Test
    void optimisticLockIs409() {
        ProblemDetail problem = handler.handleOptimisticLock(
                new ObjectOptimisticLockingFailureException(Object.class, 1L));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getProperties()).containsEntry("errorCode", "CONCURRENT_MODIFICATION");
    }

    @Test
    void dataIntegrityIs409WithoutLeakingDatabaseMessage() {
        ProblemDetail problem = handler.handleDataIntegrity(
                new DataIntegrityViolationException("ORA-00001: unique constraint (APP.UK_CUSTOMERS_EMAIL) violated"));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).doesNotContain("ORA-");
    }

    @Test
    void unexpectedExceptionIs500WithCorrelationIdAndNoMessage() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("secret internals"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).doesNotContain("secret");
        assertThat(problem.getProperties()).containsKey("correlationId");
    }
}
