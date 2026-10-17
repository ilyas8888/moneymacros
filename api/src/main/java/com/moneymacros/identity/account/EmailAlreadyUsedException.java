package com.moneymacros.identity.account;

/** Raised when signup is attempted with an email that already has an account. */
public class EmailAlreadyUsedException extends RuntimeException {

	public EmailAlreadyUsedException() {
		super("An account already exists for this email");
	}
}
