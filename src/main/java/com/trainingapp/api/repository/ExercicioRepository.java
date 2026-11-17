package com.trainingapp.api.repository;

import com.trainingapp.api.model.Exercicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExercicioRepository extends JpaRepository<Exercicio, UUID> {

	Optional<Exercicio> findBySlug(String slug);

	List<Exercicio> findBySlugIsNotNull();

	List<Exercicio> findByUsuarioId(UUID usuarioId);

	long countByUsuarioId(UUID usuarioId);

	// Grupo e busca filtrados em memoria: o Postgres rejeita parametro nulo no JPQL.
	@Query("select e from Exercicio e where e.disponivel = true and (e.usuario is null or e.usuario.id = :usuarioId) order by e.nome")
	List<Exercicio> findDisponiveis(UUID usuarioId);
}
