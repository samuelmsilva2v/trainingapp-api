package com.trainingapp.api.service;

import com.trainingapp.api.config.ErroDeAutenticacaoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimiteDeTentativasTest {

	final LimiteDeTentativas limite = new LimiteDeTentativas(Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneOffset.UTC));

	@Test
	void rajadaParalelaNaMesmaChavePassaNoMaximoOLimite() throws Exception {
		// Era o furo: checar e contar em passos separados deixava todas as requisicoes da rajada passarem pela checagem.
		int requisicoes = 200;
		ExecutorService pool = Executors.newFixedThreadPool(requisicoes);
		CountDownLatch largada = new CountDownLatch(1);
		List<Future<Boolean>> resultados = new ArrayList<>();
		for (int i = 0; i < requisicoes; i++) {
			resultados.add(pool.submit(() -> {
				largada.await();
				try {
					limite.tentar("login:maria@teste.com|1.2.3.4");
					return true;
				}
				catch (ErroDeAutenticacaoException e) {
					assertThat(e.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
					return false;
				}
			}));
		}
		largada.countDown();
		int passaram = 0;
		for (Future<Boolean> r : resultados) {
			if (r.get(10, TimeUnit.SECONDS)) {
				passaram++;
			}
		}
		pool.shutdown();

		assertThat(passaram).isEqualTo(LimiteDeTentativas.MAXIMO);
	}

	@Test
	void aUltimaTentativaPermitidaPassaEAProximaBloqueia() {
		limite.tentar("k", 3);
		limite.tentar("k", 3);
		limite.tentar("k", 3);

		assertThatThrownBy(() -> limite.tentar("k", 3)).isInstanceOf(ErroDeAutenticacaoException.class);
		limite.tentar("outra-chave", 3); // o bloqueio e por chave
	}

	@Test
	void limparLiberaAChave() {
		for (int i = 0; i < LimiteDeTentativas.MAXIMO; i++) {
			limite.tentar("k");
		}
		limite.limpar("k");

		limite.tentar("k");
	}
}
