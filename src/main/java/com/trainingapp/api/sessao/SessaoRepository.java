package com.trainingapp.api.sessao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessaoRepository extends JpaRepository<Sessao, UUID> {

	Optional<Sessao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

	Optional<Sessao> findFirstByUsuarioIdAndEstado(UUID usuarioId, EstadoSessao estado);

	List<Sessao> findByUsuarioIdAndEstadoInOrderByIniciadaEmDesc(UUID usuarioId, Collection<EstadoSessao> estados,
			Pageable pageable);
}
