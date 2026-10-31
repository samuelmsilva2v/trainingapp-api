package com.trainingapp.api.repository;

import com.trainingapp.api.model.Plano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanoRepository extends JpaRepository<Plano, UUID> {

	List<Plano> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);

	/** Id e quantidade de dias de cada plano da conta (para a conquista "Rotacao completa"). */
	@Query("select p.id, size(p.dias) from Plano p where p.usuario.id = :usuarioId")
	List<Object[]> diasPorPlano(@Param("usuarioId") UUID usuarioId);

	Optional<Plano> findByIdAndUsuarioId(UUID id, UUID usuarioId);

	Optional<Plano> findFirstByUsuarioIdAndAtivoTrue(UUID usuarioId);

	boolean existsByUsuarioIdAndAtivoTrue(UUID usuarioId);

	long countByUsuarioId(UUID usuarioId);
}
