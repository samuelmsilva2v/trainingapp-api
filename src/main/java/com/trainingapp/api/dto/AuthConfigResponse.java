package com.trainingapp.api.dto;

/** Configuracao publica para a tela de login; {@code googleClientId} vazio esconde o botao do Google. */
public record AuthConfigResponse(String googleClientId) {
}
