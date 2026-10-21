package com.moneymacros.profile.domain;

/**
 * A profile update that broke a business rule the bean-validation annotations
 * cannot express (currency must match the country, income out of range). Carries
 * the offending field so the web layer can return the same Problem Detail shape
 * as bean validation — an {@code errors} list naming the field — and the form can
 * show the message under the right input.
 */
public class InvalidProfileException extends RuntimeException {

	private final String field;

	public InvalidProfileException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
