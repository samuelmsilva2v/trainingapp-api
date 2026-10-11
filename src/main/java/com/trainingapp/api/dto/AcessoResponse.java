package com.trainingapp.api.dto;

/** Token de acesso (Bearer) e quantos segundos ele ainda vale. O refresh vai so no cookie HttpOnly. */
public record AcessoResponse(String acesso, long expiraEmSegundos) {
}
