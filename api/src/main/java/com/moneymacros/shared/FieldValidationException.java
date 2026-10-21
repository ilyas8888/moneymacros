package com.moneymacros.shared;

/**
 * A business rule that bean-validation annotations cannot express was broken for
 * a specific field (e.g. a currency that must match the country, an amount out of
 * range). Carries the offending field so the API can answer with the same RFC 9457
 * Problem Detail as bean validation — an {@code errors} list naming the field — and
 * a form can show the message under the right input. Handled once, for every
 * module, in the shared {@code GlobalExceptionHandler} (ADR 008).
 */
public class FieldValidationException extends RuntimeException {

	private final String field;

	public FieldValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
