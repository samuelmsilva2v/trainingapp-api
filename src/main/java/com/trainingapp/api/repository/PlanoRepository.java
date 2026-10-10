package com.trainingapp.api.repository;

import com.trainingapp.api.model.Plano;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanoRepository extends JpaRepository<Plano, UUID> {

	List<Plano> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);

	Optional<Plano> findByIdAndUsuarioId(UUID id, UUID usuarioId);

	Optional<Plano> findFirstByUsuarioIdAndAtivoTrue(UUID usuarioId);

	boolean existsByUsuarioIdAndAtivoTrue(UUID usuarioId);
}
