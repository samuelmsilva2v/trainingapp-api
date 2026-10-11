package com.trainingapp.api.dto;

import jakarta.validation.constraints.Size;

/** Prova de identidade para excluir a conta: a senha, ou (conta so do Google) o codigo do popup do Google. Um dos dois. */
public record ExcluirContaRequest(@Size(max = 72) String senha, @Size(max = 2048) String codigo) {
}
