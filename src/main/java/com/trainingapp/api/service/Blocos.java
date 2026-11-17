package com.trainingapp.api.service;

import com.trainingapp.api.model.Exercicio;
import com.trainingapp.api.model.ExercicioSessao;
import com.trainingapp.api.model.SerieSessao;
import com.trainingapp.api.model.TipoSerie;

import java.util.ArrayList;
import java.util.List;

public final class Blocos {

	public static final int MAX_DROPS = 3;

	private Blocos() {
	}

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

	public static int concluidos(List<SerieSessao> series) {
		return (int) de(series).stream().filter(b -> b.getFirst().isConcluida()).count();
	}

	public static int xp(ExercicioSessao exercicio) {
		Exercicio x = exercicio.getExercicio();
		int xp = 0;
		for (List<SerieSessao> bloco : de(exercicio.getSeries())) {
			if (!bloco.getFirst().isConcluida()) {
				continue;
			}
			List<SerieSessao> feitas = bloco.stream().filter(SerieSessao::isConcluida).toList();
			List<Integer> reps = feitas.stream().map(SerieSessao::getReps).toList();
			xp += x.isPesoCorporal() ? Regras.xpDoBlocoPesoCorporal(reps, x.fatorXpOuPadrao())
					: Regras.xpDoBloco(feitas.stream().map(SerieSessao::getCarga).toList(), reps);
		}
		return xp;
	}
}
