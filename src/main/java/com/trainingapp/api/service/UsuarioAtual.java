package com.trainingapp.api.service;

import com.trainingapp.api.model.Usuario;

/** Quem esta usando o app neste request: o dono do token de acesso. */
public interface UsuarioAtual {

	Usuario obter();
}
