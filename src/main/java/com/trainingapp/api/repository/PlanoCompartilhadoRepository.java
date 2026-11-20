package com.trainingapp.api.repository;

import com.trainingapp.api.model.PlanoCompartilhado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanoCompartilhadoRepository extends JpaRepository<PlanoCompartilhado, UUID> {

	Optional<PlanoCompartilhado> findByToken(String token);

	void deleteByUsuarioId(UUID usuarioId);
}
