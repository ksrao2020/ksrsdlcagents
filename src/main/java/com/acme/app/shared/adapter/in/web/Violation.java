package com.acme.app.shared.adapter.in.web;

import lombok.Builder;

/** One failed validation constraint, returned in the {@code violations} property of a problem response. */
@Builder
public record Violation(String field, String message) {
}
