package com.trainingapp.api.service;

import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.EmailPendenteRepository;
import com.trainingapp.api.repository.ExercicioRepository;
import com.trainingapp.api.repository.PlanoRepository;
import com.trainingapp.api.repository.RefreshTokenRepository;
import com.trainingapp.api.repository.SessaoRepository;
import com.trainingapp.api.repository.TokenEmailRepository;
import com.trainingapp.api.repository.UsuarioRepository;
import com.trainingapp.api.repository.XpEventRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Apaga a conta e tudo que e dela, de verdade e de uma vez: planos, treinos (com series), historico de XP (a excecao a
 * "XP nunca e apagado": sem a conta nao ha a quem o XP pertenca), exercicios proprios, logins, links de e-mail e e-mails
 * na fila. O catalogo global e os dados de outros usuarios nao sao tocados. Roda numa transacao so: ou apaga tudo ou nada.
 */
@Service
public class ExclusaoDeConta {

	private final XpEventRepository xp;
	private final SessaoRepository sessoes;
	private final PlanoRepository planos;
	private final ExercicioRepository exercicios;
	private final RefreshTokenRepository refreshTokens;
	private final TokenEmailRepository tokensEmail;
	private final EmailPendenteRepository emails;
	private final UsuarioRepository usuarios;
	private final EntityManager em;

	public ExclusaoDeConta(XpEventRepository xp, SessaoRepository sessoes, PlanoRepository planos, ExercicioRepository exercicios,
			RefreshTokenRepository refreshTokens, TokenEmailRepository tokensEmail, EmailPendenteRepository emails,
			UsuarioRepository usuarios, EntityManager em) {
		this.xp = xp;
		this.sessoes = sessoes;
		this.planos = planos;
		this.exercicios = exercicios;
		this.refreshTokens = refreshTokens;
		this.tokensEmail = tokensEmail;
		this.emails = emails;
		this.usuarios = usuarios;
		this.em = em;
	}

	@Transactional
	public void apagar(Usuario usuario) {
		UUID id = usuario.getId();
		// O cascade leva dias e exercicios dos planos e exercicios e series dos treinos.
		xp.deleteAll(xp.findByUsuarioId(id));
		sessoes.deleteAll(sessoes.findByUsuarioId(id));
		planos.deleteAll(planos.findByUsuarioIdOrderByCriadoEmDesc(id));
		// Planos e treinos referenciam os exercicios proprios: precisam sair antes deles.
		em.flush();
		exercicios.deleteAll(exercicios.findByUsuarioId(id));
		refreshTokens.deleteByUsuarioId(id);
		tokensEmail.deleteByUsuarioId(id);
		emails.deleteByDestinatario(usuario.getEmail());
		em.flush();
		usuarios.delete(usuario);
	}
}
