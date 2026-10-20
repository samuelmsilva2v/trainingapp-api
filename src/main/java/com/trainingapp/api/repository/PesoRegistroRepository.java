package com.trainingapp.api.repository;

import com.trainingapp.api.model.PesoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PesoRegistroRepository extends JpaRepository<PesoRegistro, UUID> {

	List<PesoRegistro> findByUsuarioIdOrderByDataAsc(UUID usuarioId);

	Optional<PesoRegistro> findByUsuarioIdAndData(UUID usuarioId, LocalDate data);

	void deleteByUsuarioIdAndData(UUID usuarioId, LocalDate data);

	void deleteByUsuarioId(UUID usuarioId);
}
