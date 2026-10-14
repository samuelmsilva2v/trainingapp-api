package com.trainingapp.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Prova de identidade para excluir a conta: a senha ou, em conta so do Google, o codigo do popup (web) ou o ID token do
 * Credential Manager (app Android). Um deles.
 */
public record ExcluirContaRequest(@Size(max = 72) String senha, @Size(max = 2048) String codigo, @Size(max = 4096) String idToken) {
}
