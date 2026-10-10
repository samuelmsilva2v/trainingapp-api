package com.trainingapp.api.config;

import com.trainingapp.api.service.CatalogoImportador;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "trainingapp.catalogo.importar", havingValue = "true", matchIfMissing = true)
public class CatalogoInicializador implements ApplicationRunner {

	private final CatalogoImportador importador;

	public CatalogoInicializador(CatalogoImportador importador) {
		this.importador = importador;
	}

	@Override
	public void run(ApplicationArguments args) {
		importador.importar();
	}
}
