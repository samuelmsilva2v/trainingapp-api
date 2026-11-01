package com.trainingapp.api.dto;

import com.trainingapp.api.model.CategoriaFeedback;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** O contexto (plataforma em diante) e opcional: a web nao tem versao, e a tela falta quando o Perfil foi aberto direto. */
public record FeedbackRequest(@NotBlank @Size(max = 100) String nome, @NotNull CategoriaFeedback categoria,
		@NotBlank @Size(min = 10, max = 2000) String mensagem, @Pattern(regexp = "web|android") String plataforma,
		@Size(max = 50) String versaoApp, @Size(max = 200) String tela, @Size(max = 500) String userAgent) {
}
