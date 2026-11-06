package com.trainingapp.api.repository;

import com.trainingapp.api.model.FotoPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FotoPerfilRepository extends JpaRepository<FotoPerfil, UUID> {
}
