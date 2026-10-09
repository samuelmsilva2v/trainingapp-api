package com.trainingapp.api.usuario;

/**
 * Quem esta usando o app neste request. Hoje e sempre o usuario padrao;
 * na fase de autenticacao so a implementacao muda.
 */
public interface UsuarioAtual {

	Usuario obter();
}
