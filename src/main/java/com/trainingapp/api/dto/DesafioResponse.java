package com.trainingapp.api.dto;

public record DesafioResponse(String codigo, String nome, int progresso, int meta, int xp, boolean cumprido) {
}
