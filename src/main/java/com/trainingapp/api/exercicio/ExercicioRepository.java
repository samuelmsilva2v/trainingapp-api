package com.trainingapp.api.exercicio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExercicioRepository extends JpaRepository<Exercicio, UUID> {

	Optional<Exercicio> findBySlug(String slug);

	// Catalogo global + exercicios do usuario. O filtro por grupo/busca e feito em memoria
	// (o catalogo e pequeno) para evitar parametros nulos no JPQL, que o Postgres rejeita.
	@Query("select e from Exercicio e where e.usuario is null or e.usuario.id = :usuarioId order by e.nome")
	List<Exercicio> findDisponiveis(UUID usuarioId);
}
