package com.trainingapp.api.repository;

import com.trainingapp.api.model.TipoTokenEmail;
import com.trainingapp.api.model.TokenEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenEmailRepository extends JpaRepository<TokenEmail, UUID> {

	Optional<TokenEmail> findByTokenHashAndTipo(String tokenHash, TipoTokenEmail tipo);

	void deleteByUsuarioIdAndTipo(UUID usuarioId, TipoTokenEmail tipo);
}
