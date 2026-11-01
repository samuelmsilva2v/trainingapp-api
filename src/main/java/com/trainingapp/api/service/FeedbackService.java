package com.trainingapp.api.service;

import com.trainingapp.api.config.ErroDeAutenticacaoException;
import com.trainingapp.api.config.RecursoNaoEncontradoException;
import com.trainingapp.api.dto.FeedbackRequest;
import com.trainingapp.api.model.Feedback;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

/** Grava o relato e o enfileira por e-mail para o destino configurado; sem destino, o feedback fica desligado. */
@Service
public class FeedbackService {

	static final int MAXIMO_POR_DIA = 5;
	static final int RESUMO_NO_ASSUNTO = 60;

	private final FeedbackRepository repository;
	private final FilaDeEmail fila;
	private final Clock relogio;
	private final String destino;

	public FeedbackService(FeedbackRepository repository, FilaDeEmail fila, Clock relogio,
			@Value("${trainingapp.feedback.email:}") String destino) {
		this.repository = repository;
		this.fila = fila;
		this.relogio = relogio;
		this.destino = destino;
	}

	public boolean ativo() {
		return !destino.isBlank();
	}

	@Transactional
	public void enviar(Usuario usuario, FeedbackRequest request) {
		if (!ativo()) {
			throw new RecursoNaoEncontradoException("Feedback desligado");
		}
		Instant inicioDoDia = LocalDate.now(relogio).atStartOfDay(relogio.getZone()).toInstant();
		if (repository.countByUsuarioIdAndCriadoEmGreaterThanEqual(usuario.getId(), inicioDoDia) >= MAXIMO_POR_DIA) {
			throw new ErroDeAutenticacaoException(HttpStatus.TOO_MANY_REQUESTS, "Limite de envios do dia atingido");
		}
		Feedback f = new Feedback();
		f.setUsuario(usuario);
		f.setNome(request.nome());
		f.setCategoria(request.categoria());
		f.setMensagem(request.mensagem());
		f.setPlataforma(request.plataforma());
		f.setVersaoApp(request.versaoApp());
		f.setTela(request.tela());
		f.setUserAgent(request.userAgent());
		f.setCriadoEm(Instant.now(relogio));
		repository.save(f);
		fila.enfileirar(destino, assunto(f), corpo(f, usuario.getEmail()), usuario.getEmail());
	}

	static String assunto(Feedback f) {
		String resumo = f.getMensagem().strip().replaceAll("\\s+", " ");
		if (resumo.length() > RESUMO_NO_ASSUNTO) {
			resumo = resumo.substring(0, RESUMO_NO_ASSUNTO).strip() + "…";
		}
		return "[" + f.getCategoria().rotulo() + "] " + resumo;
	}

	private static String corpo(Feedback f, String email) {
		return ("Nome: %s%nE-mail da conta: %s%nCategoria: %s%n%n%s%n%n---%nPlataforma: %s%nVersão do app: %s%nTela: %s%n"
				+ "User agent: %s%nEnviado em: %s").formatted(f.getNome(), email, f.getCategoria().rotulo(), f.getMensagem(),
						valor(f.getPlataforma()), valor(f.getVersaoApp()), valor(f.getTela()), valor(f.getUserAgent()),
						f.getCriadoEm());
	}

	private static String valor(String s) {
		return s == null || s.isBlank() ? "-" : s;
	}
}
