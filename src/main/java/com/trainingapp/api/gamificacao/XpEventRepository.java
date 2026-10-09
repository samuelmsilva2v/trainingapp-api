package com.trainingapp.api.gamificacao;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface XpEventRepository extends JpaRepository<XpEvent, UUID> {

	List<XpEvent> findByUsuarioId(UUID usuarioId);

	List<XpEvent> findBySessaoIdIn(Collection<UUID> sessaoIds);

	boolean existsBySessaoId(UUID sessaoId);

	boolean existsByUsuarioIdAndTipoAndDia(UUID usuarioId, TipoXp tipo, LocalDate dia);
}
