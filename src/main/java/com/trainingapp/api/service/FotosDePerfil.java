package com.trainingapp.api.service;

import com.trainingapp.api.config.RecursoNaoEncontradoException;
import com.trainingapp.api.model.FotoPerfil;
import com.trainingapp.api.model.FotoTipo;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.FotoPerfilRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;

/** Foto propria do perfil (api#48): enviar vira PROPRIA, remover volta a PADRAO e nao ha volta a foto do Google. */
@Service
@Transactional
public class FotosDePerfil {

	static final int TAMANHO_MAXIMO = 5 * 1024 * 1024;

	private final FotoPerfilRepository fotos;
	private final Clock relogio;

	public FotosDePerfil(FotoPerfilRepository fotos, Clock relogio) {
		this.fotos = fotos;
		this.relogio = relogio;
	}

	public void salvar(Usuario usuario, byte[] bytes) {
		if (bytes == null || bytes.length == 0 || bytes.length > TAMANHO_MAXIMO) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A foto precisa ter até 5 MB");
		}
		String tipo = formato(bytes);
		if (tipo == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma foto em JPEG, PNG ou WebP");
		}
		FotoPerfil foto = fotos.findById(usuario.getId()).orElseGet(FotoPerfil::new);
		foto.setUsuarioId(usuario.getId());
		foto.setBytes(bytes);
		foto.setContentType(tipo);
		foto.setAtualizadaEm(Instant.now(relogio));
		fotos.save(foto);
		usuario.setFotoTipo(FotoTipo.PROPRIA);
	}

	public void remover(Usuario usuario) {
		fotos.deleteById(usuario.getId());
		usuario.setFotoTipo(FotoTipo.PADRAO);
	}

	@Transactional(readOnly = true)
	public FotoPerfil obter(Usuario usuario) {
		return fotos.findById(usuario.getId()).orElseThrow(() -> new RecursoNaoEncontradoException("Sem foto própria"));
	}

	/** Pelo começo dos bytes, nao pelo Content-Type de quem enviou. */
	static String formato(byte[] b) {
		if (comeca(b, 0, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
		if (comeca(b, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) return "image/png";
		if (comeca(b, 0, 'R', 'I', 'F', 'F') && comeca(b, 8, 'W', 'E', 'B', 'P')) return "image/webp";
		return null;
	}

	private static boolean comeca(byte[] b, int inicio, int... esperado) {
		if (b.length < inicio + esperado.length) return false;
		for (int i = 0; i < esperado.length; i++) {
			if ((b[inicio + i] & 0xFF) != esperado[i]) return false;
		}
		return true;
	}
}
