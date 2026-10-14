package com.trainingapp.api.repository;

import com.trainingapp.api.model.EmailPendente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EmailPendenteRepository extends JpaRepository<EmailPendente, UUID> {

	void deleteByDestinatario(String destinatario);

	long deleteByCriadoEmBefore(Instant limite);

	List<EmailPendente> findTop20ByEnviadoEmIsNullAndTentativasLessThanAndProximaTentativaEmLessThanEqualOrderByCriadoEmAsc(
			int maxTentativas, Instant agora);
}
