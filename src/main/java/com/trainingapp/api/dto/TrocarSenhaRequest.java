package com.trainingapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code refresh}: so o app Android manda (na web vem no cookie); e o aparelho que continua logado depois da troca. */
public record TrocarSenhaRequest(@NotBlank String senhaAtual, @NotBlank @Size(max = 72) String novaSenha,
		@Size(max = 200) String refresh) {
}
