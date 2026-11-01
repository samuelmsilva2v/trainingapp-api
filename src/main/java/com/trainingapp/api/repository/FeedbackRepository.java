package com.trainingapp.api.repository;

import com.trainingapp.api.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

	long countByUsuarioIdAndCriadoEmGreaterThanEqual(UUID usuarioId, Instant desde);

	void deleteByUsuarioId(UUID usuarioId);
}
