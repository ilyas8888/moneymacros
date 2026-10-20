package com.moneymacros.identity.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneymacros.identity.AuthenticatedUser;
import com.moneymacros.identity.UserRegistered;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit test for the duplicate-email handling in {@link AccountService#register}.
 * It stubs the repository so the constraint-violation branch is exercised without
 * any timing or database; the matching integration test ({@code SignupRaceIntegrationTest})
 * proves the real {@code users_email_key} name against PostgreSQL.
 */
class AccountServiceTest {

	private final UserAccountRepository accounts = mock(UserAccountRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
	private final AccountService service = new AccountService(accounts, passwordEncoder, events);

	@Test
	void registerPersistsAndPublishesUserRegisteredOnSuccess() {
		when(accounts.existsByEmail("grad@example.com")).thenReturn(false);
		when(passwordEncoder.encode("pw")).thenReturn("hash");
		when(accounts.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		AuthenticatedUser user = service.register("Grad@Example.com", "pw");

		assertThat(user.email()).isEqualTo("grad@example.com");
		ArgumentCaptor<UserRegistered> event = ArgumentCaptor.forClass(UserRegistered.class);
		verify(events).publishEvent(event.capture());
		assertThat(event.getValue().userId()).isEqualTo(user.id());
	}

	@Test
	void registerTurnsTheEmailUniqueViolationIntoEmailAlreadyUsed() {
		when(accounts.existsByEmail("grad@example.com")).thenReturn(false);
		doReturn(null).when(passwordEncoder).encode(any());
		DataIntegrityViolationException duplicate = uniqueViolation("users_email_key");
		when(accounts.saveAndFlush(any())).thenThrow(duplicate);

		assertThatExceptionOfType(EmailAlreadyUsedException.class)
				.isThrownBy(() -> service.register("grad@example.com", "pw"));

		verify(events, never()).publishEvent(any());
	}

	@Test
	void registerLeavesOtherIntegrityViolationsAlone() {
		when(accounts.existsByEmail("grad@example.com")).thenReturn(false);
		doReturn(null).when(passwordEncoder).encode(any());
		DataIntegrityViolationException other = uniqueViolation("some_other_constraint");
		when(accounts.saveAndFlush(any())).thenThrow(other);

		assertThatExceptionOfType(DataIntegrityViolationException.class)
				.isThrownBy(() -> service.register("grad@example.com", "pw"))
				.isSameAs(other);

		verify(events, never()).publishEvent(any());
	}

	/**
	 * A {@link DataIntegrityViolationException} wrapping a Hibernate
	 * {@code ConstraintViolationException} that reports the given constraint name,
	 * the way Spring Data translates a PostgreSQL unique violation. The name is
	 * stubbed on a mock so this test does not depend on Hibernate's constructor.
	 */
	private static DataIntegrityViolationException uniqueViolation(String constraintName) {
		org.hibernate.exception.ConstraintViolationException hibernate =
				mock(org.hibernate.exception.ConstraintViolationException.class);
		when(hibernate.getConstraintName()).thenReturn(constraintName);
		return new DataIntegrityViolationException("duplicate key", hibernate);
	}
}
