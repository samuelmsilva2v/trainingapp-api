package com.trainingapp.api.service;

import java.util.Map;

/** Temas do app e o nível mínimo de cada um (0 = livre). Espelha `NIVEL_DO_TEMA` em `core/tema.ts` do web. */
public final class Temas {

	private static final Map<String, Integer> NIVEL_MINIMO = Map.ofEntries(
			Map.entry("escuro", 0), Map.entry("claro", 0),
			Map.entry("chrome", 5), Map.entry("electric", 10), Map.entry("ignite", 15),
			Map.entry("commando-deserto", 20), Map.entry("commando-mata", 20), Map.entry("cyberpunk", 25),
			Map.entry("japao-sakura", 30), Map.entry("japao-hokusai", 30), Map.entry("japao-sumie", 30));

	private Temas() {
	}

	public static boolean existe(String tema) {
		return NIVEL_MINIMO.containsKey(tema);
	}

	public static boolean liberado(String tema, int nivel) {
		return nivel >= NIVEL_MINIMO.get(tema);
	}
}
