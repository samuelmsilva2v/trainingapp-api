package com.trainingapp.api.controller;

import com.trainingapp.api.config.RegraDeNegocioException;
import com.trainingapp.api.dto.PesoRequest;
import com.trainingapp.api.dto.PesoResponse;
import com.trainingapp.api.model.PesoRegistro;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.PesoRegistroRepository;
import com.trainingapp.api.service.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Pesagens do usuario, uma por dia. Retroativo e permitido; data futura (no fuso do sistema) nao. */
@RestController
@RequestMapping("/api/peso")
public class PesoController {

	private final UsuarioAtual usuarioAtual;
	private final PesoRegistroRepository repository;
	private final Clock relogio;

	public PesoController(UsuarioAtual usuarioAtual, PesoRegistroRepository repository, Clock relogio) {
		this.usuarioAtual = usuarioAtual;
		this.repository = repository;
		this.relogio = relogio;
	}

	@GetMapping
	public List<PesoResponse> listar() {
		return repository.findByUsuarioIdOrderByDataAsc(usuarioAtual.obter().getId()).stream().map(PesoResponse::de).toList();
	}

	@PutMapping("/{data}")
	@Transactional
	public PesoResponse registrar(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
			@Valid @RequestBody PesoRequest request) {
		if (data.isAfter(LocalDate.now(relogio))) {
			throw new RegraDeNegocioException("A data da pesagem nao pode ser futura");
		}
		Usuario usuario = usuarioAtual.obter();
		PesoRegistro registro = repository.findByUsuarioIdAndData(usuario.getId(), data).orElseGet(() -> {
			PesoRegistro novo = new PesoRegistro();
			novo.setUsuario(usuario);
			novo.setData(data);
			return novo;
		});
		registro.setKg(request.kg());
		return PesoResponse.de(repository.save(registro));
	}

	@DeleteMapping("/{data}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void excluir(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
		repository.deleteByUsuarioIdAndData(usuarioAtual.obter().getId(), data);
	}
}
