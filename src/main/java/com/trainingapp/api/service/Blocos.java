package com.trainingapp.api.service;

import com.trainingapp.api.model.SerieSessao;
import com.trainingapp.api.model.TipoSerie;

import java.util.ArrayList;
import java.util.List;

/**
 * Agrupa as series de um exercicio em blocos: uma serie normal seguida dos drops dela. Um bloco conta como uma
 * serie (no "X/Y", nas series minimas do treino e no XP); o servidor ja grava os drops com o concluida da serie normal.
 */
public final class Blocos {

	/** Maximo de drops depois de uma serie, no plano e na execucao. */
	public static final int MAX_DROPS = 3;

	private Blocos() {
	}

	/** As series ja em ordem; um drop sem serie normal antes dele nao forma bloco (a validacao do PUT o rejeita). */
	public static List<List<SerieSessao>> de(List<SerieSessao> series) {
		List<List<SerieSessao>> blocos = new ArrayList<>();
		for (SerieSessao s : series) {
			if (s.getTipo() == TipoSerie.DROP && !blocos.isEmpty()) {
				blocos.getLast().add(s);
			}
			else if (s.getTipo() != TipoSerie.DROP) {
				blocos.add(new ArrayList<>(List.of(s)));
			}
		}
		return blocos;
	}

	/** Blocos cuja serie normal foi concluida. */
	public static int concluidos(List<SerieSessao> series) {
		return (int) de(series).stream().filter(b -> b.getFirst().isConcluida()).count();
	}

	/** XP dos blocos concluidos: cada um vale como uma serie, pelo volume somado das etapas. */
	public static int xp(List<SerieSessao> series) {
		int xp = 0;
		for (List<SerieSessao> bloco : de(series)) {
			if (!bloco.getFirst().isConcluida()) {
				continue;
			}
			List<SerieSessao> feitas = bloco.stream().filter(SerieSessao::isConcluida).toList();
			xp += Regras.xpDoBloco(feitas.stream().map(SerieSessao::getCarga).toList(),
					feitas.stream().map(SerieSessao::getReps).toList());
		}
		return xp;
	}
}
