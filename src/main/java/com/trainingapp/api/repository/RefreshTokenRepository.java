package com.trainingapp.api.repository;

import com.trainingapp.api.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	void deleteByUsuarioId(UUID usuarioId);

	long deleteByUltimoUsoEmBefore(Instant limite);

	void deleteByUsuarioIdAndIdNot(UUID usuarioId, UUID id);
}
