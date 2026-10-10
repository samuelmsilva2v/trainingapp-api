package com.trainingapp.api.service;

import com.trainingapp.api.model.Usuario;

/**
 * Quem esta usando o app neste request. Hoje e sempre o usuario padrao;
 * na fase de autenticacao so a implementacao muda.
 */
public interface UsuarioAtual {

	Usuario obter();
}
