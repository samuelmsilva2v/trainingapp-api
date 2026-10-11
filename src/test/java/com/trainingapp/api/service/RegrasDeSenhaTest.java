package com.trainingapp.api.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasDeSenhaTest {

	@Test
	void aceitaSenhaComTodosOsCriterios() {
		assertThat(RegrasDeSenha.problema("Senha-Forte-123!")).isNull();
		assertThat(RegrasDeSenha.problema("Aa1!aaaa")).isNull(); // exatamente 8
		assertThat(RegrasDeSenha.problema("Çedilha Ñ 7 ç")).isNull(); // acentos contam como letras; espaco conta como simbolo
	}

	@Test
	void recusaCadaCriterioQueFalta() {
		assertThat(RegrasDeSenha.problema("Aa1!aaa")).isEqualTo(RegrasDeSenha.MENSAGEM); // 7 caracteres
		assertThat(RegrasDeSenha.problema("sem-maiuscula-1!")).isEqualTo(RegrasDeSenha.MENSAGEM);
		assertThat(RegrasDeSenha.problema("SEM-MINUSCULA-1!")).isEqualTo(RegrasDeSenha.MENSAGEM);
		assertThat(RegrasDeSenha.problema("Sem-Numero-Aqui!")).isEqualTo(RegrasDeSenha.MENSAGEM);
		assertThat(RegrasDeSenha.problema("SemSimbolo123")).isEqualTo(RegrasDeSenha.MENSAGEM);
		assertThat(RegrasDeSenha.problema("")).isEqualTo(RegrasDeSenha.MENSAGEM);
	}

	@Test
	void recusaSenhaMaiorQueOBcryptLe() {
		assertThat(RegrasDeSenha.problema("Aa1!" + "a".repeat(68))).isNull(); // 72 bytes
		assertThat(RegrasDeSenha.problema("Aa1!" + "a".repeat(69))).contains("72 bytes");
		assertThat(RegrasDeSenha.problema("Aa1!" + "é".repeat(35))).contains("72 bytes"); // 74 bytes com 39 caracteres
	}
}
