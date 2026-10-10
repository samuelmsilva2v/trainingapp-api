package com.trainingapp.api.service;

import com.trainingapp.api.model.EstadoSessao;
import com.trainingapp.api.model.Sessao;
import com.trainingapp.api.model.TipoXp;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.model.XpEvent;
import com.trainingapp.api.repository.SessaoRepository;
import com.trainingapp.api.repository.XpEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** As regras de unicidade valem no banco, nao so na checagem de leitura (corrida entre dois envios). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GarantiasDoBancoTest {

	@Autowired
	UsuarioAtual usuarioAtual;

	@Autowired
	XpEventRepository eventos;

	@Autowired
	SessaoRepository sessoes;

	@Test
	void doisEventosDeSessaoNoMesmoDiaViolamAChaveUnica() {
		Usuario usuario = usuarioAtual.obter();
		eventos.saveAndFlush(eventoSessao(usuario));

		assertThrows(DataIntegrityViolationException.class, () -> eventos.saveAndFlush(eventoSessao(usuario)));
	}

	@Test
	void duasSessoesEmAndamentoDoMesmoUsuarioViolamAChaveUnica() {
		Usuario usuario = usuarioAtual.obter();
		sessoes.saveAndFlush(emAndamento(usuario));

		assertThrows(DataIntegrityViolationException.class, () -> sessoes.saveAndFlush(emAndamento(usuario)));
	}

	private static XpEvent eventoSessao(Usuario usuario) {
		XpEvent e = new XpEvent();
		e.setUsuario(usuario);
		e.setSessaoId(UUID.randomUUID());
		e.setTipo(TipoXp.SESSAO);
		e.setPontos(50);
		e.setDia(LocalDate.of(2026, 1, 1));
		e.setDiaUnico(LocalDate.of(2026, 1, 1));
		return e;
	}

	private static Sessao emAndamento(Usuario usuario) {
		Sessao s = new Sessao();
		s.setId(UUID.randomUUID());
		s.setUsuario(usuario);
		s.setPlanoNome("P");
		s.setDiaNome("A");
		s.setEstado(EstadoSessao.EM_ANDAMENTO);
		s.setAndamentoUsuarioId(usuario.getId());
		s.setIniciadaEm(Instant.now());
		s.setAtualizadoEm(Instant.now());
		return s;
	}
}
