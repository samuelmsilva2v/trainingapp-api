package com.trainingapp.api.controller;

import com.trainingapp.api.config.RegraDeNegocioException;
import com.trainingapp.api.dto.PerfilRequest;
import com.trainingapp.api.dto.PerfilResponse;
import com.trainingapp.api.dto.TemaRequest;
import com.trainingapp.api.model.FotoPerfil;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.UsuarioRepository;
import com.trainingapp.api.service.FeedbackService;
import com.trainingapp.api.service.FotosDePerfil;
import com.trainingapp.api.service.GamificacaoService;
import com.trainingapp.api.service.Temas;
import com.trainingapp.api.service.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

	private final UsuarioAtual usuarioAtual;
	private final UsuarioRepository repository;
	private final FeedbackService feedback;
	private final FotosDePerfil fotos;
	private final GamificacaoService gamificacao;

	public PerfilController(UsuarioAtual usuarioAtual, UsuarioRepository repository, FeedbackService feedback, FotosDePerfil fotos,
			GamificacaoService gamificacao) {
		this.usuarioAtual = usuarioAtual;
		this.repository = repository;
		this.feedback = feedback;
		this.fotos = fotos;
		this.gamificacao = gamificacao;
	}

	@GetMapping
	public PerfilResponse obter() {
		return PerfilResponse.de(usuarioAtual.obter(), feedback.ativo());
	}

	@PutMapping
	@Transactional
	public PerfilResponse atualizar(@Valid @RequestBody PerfilRequest request) {
		Usuario usuario = usuarioAtual.obter();
		usuario.setNome(request.nome());
		usuario.setLembretePesoDias(request.lembretePesoDias());
		return PerfilResponse.de(repository.save(usuario), feedback.ativo());
	}

	/** O tema acompanha a conta em qualquer aparelho; só aceita tema já liberado pelo nível. */
	@PutMapping("/tema")
	@Transactional
	public PerfilResponse escolherTema(@Valid @RequestBody TemaRequest request) {
		if (!Temas.existe(request.tema())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tema desconhecido");
		}
		Usuario usuario = usuarioAtual.obter();
		if (!Temas.liberado(request.tema(), gamificacao.nivel(usuario))) {
			throw new RegraDeNegocioException("Tema ainda nao liberado para o seu nivel");
		}
		usuario.setTema(request.tema());
		return PerfilResponse.de(repository.save(usuario), feedback.ativo());
	}

	@GetMapping("/foto")
	public ResponseEntity<byte[]> foto() {
		FotoPerfil foto = fotos.obter(usuarioAtual.obter());
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.getContentType()))
				.cacheControl(CacheControl.noStore()).body(foto.getBytes());
	}

	@PutMapping("/foto")
	@Transactional
	public PerfilResponse enviarFoto(@RequestBody byte[] imagem) {
		Usuario usuario = usuarioAtual.obter();
		fotos.salvar(usuario, imagem);
		return PerfilResponse.de(repository.save(usuario), feedback.ativo());
	}

	@DeleteMapping("/foto")
	@Transactional
	public PerfilResponse removerFoto() {
		Usuario usuario = usuarioAtual.obter();
		fotos.remover(usuario);
		return PerfilResponse.de(repository.save(usuario), feedback.ativo());
	}
}
