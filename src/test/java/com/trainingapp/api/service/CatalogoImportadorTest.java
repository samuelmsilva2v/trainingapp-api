package com.trainingapp.api.service;

import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.Exercicio;
import com.trainingapp.api.model.GrupoMuscular;
import com.trainingapp.api.repository.ExercicioRepository;
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
		assertThat(total).isEqualTo(876);
		assertThat(repository.count()).isEqualTo(total);

		importador.importar();
		assertThat(repository.count()).isEqualTo(total);
	}

	@Test
	void reimportarRestauraOCatalogoSemTocarNosExerciciosDoUsuario() {
		importador.importar();

		Exercicio supino = repository.findBySlug("Barbell_Bench_Press_-_Medium_Grip").orElseThrow();
		supino.setNome("Nome alterado");

		Exercicio proprio = new Exercicio();
		proprio.setNome("Meu exercicio");
		proprio.setGrupoMuscular(GrupoMuscular.COSTAS);
		proprio.setEquipamento(Equipamento.OUTRO);
		proprio.setUsuario(usuarioAtual.obter());
		repository.saveAndFlush(proprio);

		importador.importar();

		assertThat(repository.findBySlug("Barbell_Bench_Press_-_Medium_Grip").orElseThrow().getNome())
				.isEqualTo("Supino reto com barra");
		assertThat(repository.findById(proprio.getId()).orElseThrow().getNome()).isEqualTo("Meu exercicio");
	}
}
