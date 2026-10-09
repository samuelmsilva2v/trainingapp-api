package com.trainingapp.api.exercicio;

import com.trainingapp.api.usuario.UsuarioAtual;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogoImportadorTest {

	@Autowired
	CatalogoImportador importador;

	@Autowired
	ExercicioRepository repository;

	@Autowired
	UsuarioAtual usuarioAtual;

	@Test
	void importarEhIdempotente() {
		int total = importador.importar();
		assertThat(total).isGreaterThan(30);
		assertThat(repository.count()).isEqualTo(total);

		importador.importar();
		assertThat(repository.count()).isEqualTo(total);
	}

	@Test
	void reimportarRestauraOCatalogoSemTocarNosExerciciosDoUsuario() {
		importador.importar();

		Exercicio supino = repository.findBySlug("supino-reto-barra").orElseThrow();
		supino.setNome("Nome alterado");

		Exercicio proprio = new Exercicio();
		proprio.setNome("Meu exercicio");
		proprio.setGrupoMuscular(GrupoMuscular.COSTAS);
		proprio.setEquipamento(Equipamento.OUTRO);
		proprio.setUsuario(usuarioAtual.obter());
		repository.saveAndFlush(proprio);

		importador.importar();

		assertThat(repository.findBySlug("supino-reto-barra").orElseThrow().getNome())
				.isEqualTo("Supino reto com barra");
		assertThat(repository.findById(proprio.getId()).orElseThrow().getNome()).isEqualTo("Meu exercicio");
	}
}
