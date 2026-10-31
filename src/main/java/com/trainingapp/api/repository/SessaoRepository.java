package com.trainingapp.api.repository;

import com.trainingapp.api.model.EstadoSessao;
import com.trainingapp.api.model.ExercicioSessao;
import com.trainingapp.api.model.SerieSessao;
import com.trainingapp.api.model.Sessao;
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

	List<Sessao> findByUsuarioId(UUID usuarioId);

	Optional<Sessao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

	Optional<Sessao> findFirstByUsuarioIdAndEstado(UUID usuarioId, EstadoSessao estado);

	List<Sessao> findByUsuarioIdAndEstadoInOrderByIniciadaEmDesc(UUID usuarioId, Collection<EstadoSessao> estados,
			Pageable pageable);

	/** Ocorrencias do exercicio em treinos concluidos, da mais recente para a mais antiga. */
	@Query("""
			select e from ExercicioSessao e join fetch e.sessao s
			where s.usuario.id = :usuarioId and s.estado = com.trainingapp.api.model.EstadoSessao.CONCLUIDA
			  and e.exercicio.id = :exercicioId
			order by s.iniciadaEm desc
			""")
	List<ExercicioSessao> ocorrenciasConcluidas(@Param("usuarioId") UUID usuarioId,
			@Param("exercicioId") UUID exercicioId, Pageable pageable);

	/** Maior carga ja levantada (com pelo menos 1 repeticao) no exercicio, em outras sessoes concluidas. */
	@Query("""
			select max(s.carga) from SerieSessao s
			where s.concluida = true and s.reps >= 1
			  and s.exercicioSessao.exercicio.id = :exercicioId
			  and s.exercicioSessao.sessao.usuario.id = :usuarioId
			  and s.exercicioSessao.sessao.estado = com.trainingapp.api.model.EstadoSessao.CONCLUIDA
			  and s.exercicioSessao.sessao.id <> :sessaoId
			""")
	BigDecimal maiorCargaAnterior(@Param("usuarioId") UUID usuarioId, @Param("exercicioId") UUID exercicioId,
			@Param("sessaoId") UUID sessaoId);

	/**
	 * Treinos concluidos da conta, mais o treino {@code atual} (a previa de um em andamento). Sem parametro nulo: sem
	 * treino atual, passe um id que nao existe.
	 */
	@Query("""
			select new com.trainingapp.api.repository.TreinoDaConta(s.id, s.finalizadaEm, s.planoId, s.diaOrdem)
			from Sessao s
			where s.usuario.id = :usuarioId and (s.estado = com.trainingapp.api.model.EstadoSessao.CONCLUIDA or s.id = :atual)
			""")
	List<TreinoDaConta> treinosDaConta(@Param("usuarioId") UUID usuarioId, @Param("atual") UUID atual);

	/** Cada exercicio dos treinos acima, somado no banco (series concluidas com pelo menos 1 repeticao). */
	@Query("""
			select new com.trainingapp.api.repository.ExercicioDoTreino(s.id, x.id, x.grupoMuscular, x.equipamento,
					sum(r.carga * r.reps), max(r.carga))
			from SerieSessao r join r.exercicioSessao e join e.sessao s join e.exercicio x
			where s.usuario.id = :usuarioId and (s.estado = com.trainingapp.api.model.EstadoSessao.CONCLUIDA or s.id = :atual)
			  and r.concluida = true and r.reps >= 1
			group by s.id, x.id, x.grupoMuscular, x.equipamento
			""")
	List<ExercicioDoTreino> exerciciosDaConta(@Param("usuarioId") UUID usuarioId, @Param("atual") UUID atual);

	/**
	 * Series (sessao, exercicio da sessao, ordem, tipo, concluida) so dos exercicios com drop concluido: o drop-set e raro,
	 * entao isto traz poucas linhas, ja na ordem para montar os blocos.
	 */
	@Query("""
			select e.sessao.id, e.id, r.ordem, r.tipo, r.concluida
			from SerieSessao r join r.exercicioSessao e
			where e.id in (select d.exercicioSessao.id from SerieSessao d
					where d.tipo = com.trainingapp.api.model.TipoSerie.DROP and d.concluida = true
					  and d.exercicioSessao.sessao.usuario.id = :usuarioId)
			order by e.id, r.ordem
			""")
	List<Object[]> seriesComDrop(@Param("usuarioId") UUID usuarioId);

	@Query("select count(e) from ExercicioSessao e where e.id in :ids and e.sessao.id <> :sessaoId")
	long contarExerciciosDeOutraSessao(@Param("ids") Collection<UUID> ids, @Param("sessaoId") UUID sessaoId);

	@Query("select count(s) from SerieSessao s where s.id in :ids and s.exercicioSessao.sessao.id <> :sessaoId")
	long contarSeriesDeOutraSessao(@Param("ids") Collection<UUID> ids, @Param("sessaoId") UUID sessaoId);
}
