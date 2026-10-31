package com.trainingapp.api.dto;

/** Desafio da semana: `progresso` vai de 0 ate `meta`; `cumprido` tambem vale se o XP ja foi creditado na semana. */
public record DesafioResponse(String codigo, String nome, int progresso, int meta, int xp, boolean cumprido) {
}
