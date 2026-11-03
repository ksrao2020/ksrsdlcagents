package com.acme.app.shared.adapter.in.web;

import com.acme.app.shared.domain.exception.BusinessRuleViolationException;
import com.acme.app.shared.domain.exception.ConflictException;
import com.acme.app.shared.domain.exception.DomainException;
import com.acme.app.shared.domain.exception.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The single place that turns exceptions into RFC 9457 problem responses. Every response carries a stable
 * {@code errorCode} and a {@code timestamp}. Stories add {@link DomainException} subclasses, not handler methods.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String ERROR_CODE = "errorCode";
    static final String VALIDATION_FAILED = "VALIDATION_FAILED";

    private static final Map<Integer, String> DEFAULT_CODES = Map.of(
            400, "MALFORMED_REQUEST",
            404, "RESOURCE_NOT_FOUND",
            405, "METHOD_NOT_ALLOWED",
            406, "NOT_ACCEPTABLE",
            415, "UNSUPPORTED_MEDIA_TYPE");

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return domainProblem(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return domainProblem(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        return domainProblem(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<Violation> violations = ex.getConstraintViolations().stream()
                .map(v -> Violation.builder()
                        .field(lastNode(v.getPropertyPath().toString()))
                        .message(v.getMessage())
                        .build())
                .toList();
        return validationProblem(violations);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The resource was modified by another request. Reload it and retry.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        // Never expose the database message: it can leak schema details.
        return problem(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "The request conflicts with existing data.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You are not allowed to perform this operation.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("Unhandled exception [correlationId={}]", correlationId, ex);
        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.");
        problem.setProperty("correlationId", correlationId);
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Violation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Violation.builder().field(error.getField()).message(error.getDefaultMessage()).build())
                .toList();
        return handleExceptionInternal(ex, validationProblem(violations), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Violation> violations = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> Violation.builder()
                                .field(result.getMethodParameter().getParameterName())
                                .message(error.getDefaultMessage())
                                .build()))
                .toList();
        return handleExceptionInternal(ex, validationProblem(violations), headers, status, request);
    }

    /** Adds {@code errorCode} and {@code timestamp} to the problems built by Spring MVC itself (405, 415, ...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            Map<String, Object> properties = problem.getProperties();
            if (properties == null || !properties.containsKey(ERROR_CODE)) {
                problem.setProperty(ERROR_CODE, DEFAULT_CODES.getOrDefault(statusCode.value(), "HTTP_" + statusCode.value()));
                problem.setProperty("timestamp", OffsetDateTime.now());
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static ProblemDetail domainProblem(HttpStatus status, DomainException ex) {
        return problem(status, ex.getErrorCode(), ex.getMessage());
    }

    private static ProblemDetail validationProblem(List<Violation> violations) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, "Request validation failed.");
        problem.setProperty("violations", violations);
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(ERROR_CODE, errorCode);
        problem.setProperty("timestamp", OffsetDateTime.now());
        return problem;
    }

    private static String lastNode(String propertyPath) {
        int dot = propertyPath.lastIndexOf('.');
        return dot < 0 ? propertyPath : propertyPath.substring(dot + 1);
    }
}
