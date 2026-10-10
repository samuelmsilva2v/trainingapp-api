package com.trainingapp.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Serve o front compilado (a pasta {@code dist/.../browser} do Angular) pela propria API: o app inteiro fica numa
 * origem so ({@code /} = app, {@code /api} = API), sem CORS, e um unico tunel expoe tudo. So liga quando
 * {@code trainingapp.web.pasta} esta definida (perfil {@code online}).
 */
@Configuration
@ConditionalOnProperty("trainingapp.web.pasta")
public class FrontEstaticoConfig implements WebMvcConfigurer {

	private final Path pasta;

	public FrontEstaticoConfig(@Value("${trainingapp.web.pasta}") String pasta) {
		this.pasta = Path.of(pasta).toAbsolutePath().normalize();
	}

	// O handler de recursos ignora o caminho vazio, entao a raiz e encaminhada para o index.html.
	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("forward:/index.html");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String local = pasta.toUri().toString();
		Resource indice = new FileSystemResource(pasta.resolve("index.html"));
		registry.addResourceHandler("/**")
				.addResourceLocations(local.endsWith("/") ? local : local + "/")
				// O build troca os arquivos com hash no nome: o navegador sempre revalida, senao fica com um index.html velho.
				.setCacheControl(CacheControl.noCache())
				.resourceChain(false)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String caminho, Resource base) throws IOException {
						if (caminho.startsWith("api/")) {
							return null; // rota de API que nao existe: 404, nunca o index.html
						}
						Resource arquivo = base.createRelative(caminho);
						if (arquivo.isReadable()) {
							return arquivo;
						}
						// Rotas do Angular (/planos/123) nao sao arquivos: devolve o index.html e o roteador resolve.
						return indice.isReadable() ? indice : null;
					}
				});
	}
}
