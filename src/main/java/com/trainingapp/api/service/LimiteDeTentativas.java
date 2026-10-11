package com.trainingapp.api.service;

import com.trainingapp.api.config.ErroDeAutenticacaoException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freio simples contra forca bruta: {@value #MAXIMO} tentativas por chave dentro de 15 minutos bloqueiam a chave por 15 minutos.
 * shortcut: contador em memoria, vale para uma instancia so e zera ao reiniciar; com mais de uma instancia, mover para o banco ou Redis.
 */
@Component
public class LimiteDeTentativas {

	static final int MAXIMO = 5;
	static final Duration JANELA = Duration.ofMinutes(15);

	private record Contagem(int tentativas, Instant desde, Instant bloqueadoAte) {
	}

	private final ConcurrentHashMap<String, Contagem> contagens = new ConcurrentHashMap<>();
	private final Clock relogio;

	public LimiteDeTentativas(Clock relogio) {
		this.relogio = relogio;
	}

	/** 429 se a chave esta bloqueada. */
	public void exigirLivre(String chave) {
		Contagem c = contagens.get(chave);
		if (c != null && c.bloqueadoAte() != null && c.bloqueadoAte().isAfter(Instant.now(relogio))) {
			throw new ErroDeAutenticacaoException(HttpStatus.TOO_MANY_REQUESTS,
					"Muitas tentativas. Aguarde alguns minutos e tente de novo.");
		}
	}

	/** Conta uma tentativa; na quinta, bloqueia. */
	public void registrar(String chave) {
		Instant agora = Instant.now(relogio);
		if (contagens.size() > 10_000) {
			contagens.values().removeIf(c -> c.desde().plus(JANELA).isBefore(agora));
		}
		contagens.compute(chave, (k, atual) -> {
			if (atual == null || atual.desde().plus(JANELA).isBefore(agora)) {
				atual = new Contagem(0, agora, null);
			}
			int n = atual.tentativas() + 1;
			return new Contagem(n, atual.desde(), n >= MAXIMO ? agora.plus(JANELA) : atual.bloqueadoAte());
		});
	}

	public void limpar(String chave) {
		contagens.remove(chave);
	}
}
