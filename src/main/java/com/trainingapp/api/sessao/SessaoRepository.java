package com.trainingapp.api.sessao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessaoRepository extends JpaRepository<Sessao, UUID> {

	Optional<Sessao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

	Optional<Sessao> findFirstByUsuarioIdAndEstado(UUID usuarioId, EstadoSessao estado);

	List<Sessao> findByUsuarioIdAndEstadoInOrderByIniciadaEmDesc(UUID usuarioId, Collection<EstadoSessao> estados,
			Pageable pageable);

	/** Maior carga ja levantada (com pelo menos 1 repeticao) no exercicio, em outras sessoes concluidas. */
	@Query("""
			select max(s.carga) from SerieSessao s
			where s.concluida = true and s.reps >= 1
			  and s.exercicioSessao.exercicio.id = :exercicioId
			  and s.exercicioSessao.sessao.usuario.id = :usuarioId
			  and s.exercicioSessao.sessao.estado = com.trainingapp.api.sessao.EstadoSessao.CONCLUIDA
			  and s.exercicioSessao.sessao.id <> :sessaoId
			""")
	BigDecimal maiorCargaAnterior(@Param("usuarioId") UUID usuarioId, @Param("exercicioId") UUID exercicioId,
			@Param("sessaoId") UUID sessaoId);
}
