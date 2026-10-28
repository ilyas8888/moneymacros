package com.moneymacros.profile.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

	/** Every query is scoped to the owner, so no user can read another's profile. */
	Optional<Profile> findByUserId(UUID userId);
}
