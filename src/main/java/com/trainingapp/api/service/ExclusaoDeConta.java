package com.trainingapp.api.service;

import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.EmailPendenteRepository;
import com.trainingapp.api.repository.ExercicioRepository;
import com.trainingapp.api.repository.FeedbackRepository;
import com.trainingapp.api.repository.FotoPerfilRepository;
import com.trainingapp.api.repository.PesoRegistroRepository;
import com.trainingapp.api.repository.PlanoCompartilhadoRepository;
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

@Service
public class ExclusaoDeConta {

	private final XpEventRepository xp;
	private final SessaoRepository sessoes;
	private final PlanoRepository planos;
	private final ExercicioRepository exercicios;
	private final PesoRegistroRepository pesos;
	private final RefreshTokenRepository refreshTokens;
	private final TokenEmailRepository tokensEmail;
	private final EmailPendenteRepository emails;
	private final FeedbackRepository feedbacks;
	private final UsuarioRepository usuarios;
	private final EntityManager em;
	private final FotoPerfilRepository fotos;
	private final PlanoCompartilhadoRepository compartilhados;

	public ExclusaoDeConta(XpEventRepository xp, SessaoRepository sessoes, PlanoRepository planos, ExercicioRepository exercicios,
			PesoRegistroRepository pesos, RefreshTokenRepository refreshTokens, TokenEmailRepository tokensEmail,
			EmailPendenteRepository emails, FeedbackRepository feedbacks, UsuarioRepository usuarios, EntityManager em,
			FotoPerfilRepository fotos, PlanoCompartilhadoRepository compartilhados) {
		this.compartilhados = compartilhados;
		this.xp = xp;
		this.sessoes = sessoes;
		this.planos = planos;
		this.exercicios = exercicios;
		this.pesos = pesos;
		this.refreshTokens = refreshTokens;
		this.tokensEmail = tokensEmail;
		this.emails = emails;
		this.feedbacks = feedbacks;
		this.usuarios = usuarios;
		this.em = em;
		this.fotos = fotos;
	}

	@Transactional
	public void apagar(Usuario usuario) {
		UUID id = usuario.getId();
		xp.deleteAll(xp.findByUsuarioId(id));
		sessoes.deleteAll(sessoes.findByUsuarioId(id));
		planos.deleteAll(planos.findByUsuarioIdOrderByCriadoEmDesc(id));
		// Planos e treinos referenciam os exercicios proprios: precisam sair antes deles.
		em.flush();
		exercicios.deleteAll(exercicios.findByUsuarioId(id));
		pesos.deleteByUsuarioId(id);
		refreshTokens.deleteByUsuarioId(id);
		tokensEmail.deleteByUsuarioId(id);
		emails.deleteByDestinatario(usuario.getEmail());
		emails.deleteByResponderPara(usuario.getEmail());
		feedbacks.deleteByUsuarioId(id);
		fotos.deleteById(id);
		compartilhados.deleteByUsuarioId(id);
		em.flush();
		usuarios.delete(usuario);
	}
}
