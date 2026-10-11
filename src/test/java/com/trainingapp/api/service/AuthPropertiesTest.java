package com.trainingapp.api.service;

import com.trainingapp.api.config.AuthProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPropertiesTest {

	@Test
	void listaDeEmailsIgnoraCaixaEEspacosEListaVaziaNaoLiberaNinguem() {
		AuthProperties props = new AuthProperties("x", List.of(" Maria@Teste.com ", ""), null, null, false);

		assertThat(props.permitido("maria@teste.com")).isTrue();
		assertThat(props.permitido("MARIA@TESTE.COM ")).isTrue();
		assertThat(props.permitido("outra@teste.com")).isFalse();
		assertThat(new AuthProperties("x", null, null, null, false).permitido("maria@teste.com")).isFalse();
	}

	@Test
	void loginComGoogleSoLigaComClientIdESegredoJuntos() {
		assertThat(new AuthProperties("x", null, "id", "segredo", false).googleConfigurado()).isTrue();
		assertThat(new AuthProperties("x", null, "id", "", false).googleConfigurado()).isFalse();
		assertThat(new AuthProperties("x", null, "", "segredo", false).googleConfigurado()).isFalse();
		assertThat(new AuthProperties("x", null, null, null, false).googleConfigurado()).isFalse();
	}
}
