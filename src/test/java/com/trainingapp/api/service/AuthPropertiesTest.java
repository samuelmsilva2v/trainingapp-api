package com.trainingapp.api.service;

import com.trainingapp.api.config.AuthProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPropertiesTest {

	@Test
	void listaDeEmailsIgnoraCaixaEEspacosEListaVaziaNaoLiberaNinguem() {
		AuthProperties props = new AuthProperties("x", List.of(" Maria@Teste.com ", ""), null, null, false, false);

		assertThat(props.permitido("maria@teste.com")).isTrue();
		assertThat(props.permitido("MARIA@TESTE.COM ")).isTrue();
		assertThat(props.permitido("outra@teste.com")).isFalse();
		assertThat(new AuthProperties("x", null, null, null, false, false).permitido("maria@teste.com")).isFalse();
	}

	@Test
	void cadastroAbertoLiberaQualquerEmailMesmoSemLista() {
		assertThat(new AuthProperties("x", null, null, null, false, true).permitido("qualquer@teste.com")).isTrue();
		assertThat(new AuthProperties("x", List.of("maria@teste.com"), null, null, false, true).permitido("outra@teste.com")).isTrue();
	}

	@Test
	void loginComGoogleSoLigaComClientIdESegredoJuntos() {
		assertThat(new AuthProperties("x", null, "id", "segredo", false, false).googleConfigurado()).isTrue();
		assertThat(new AuthProperties("x", null, "id", "", false, false).googleConfigurado()).isFalse();
		assertThat(new AuthProperties("x", null, "", "segredo", false, false).googleConfigurado()).isFalse();
		assertThat(new AuthProperties("x", null, null, null, false, false).googleConfigurado()).isFalse();
	}
}
