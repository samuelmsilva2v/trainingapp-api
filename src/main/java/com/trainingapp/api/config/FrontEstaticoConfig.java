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

@Configuration
@ConditionalOnProperty("trainingapp.web.pasta")
public class FrontEstaticoConfig implements WebMvcConfigurer {

	private final Path pasta;

	public FrontEstaticoConfig(@Value("${trainingapp.web.pasta}") String pasta) {
		this.pasta = Path.of(pasta).toAbsolutePath().normalize();
	}

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
				// Sempre revalida, senao o navegador fica com um index.html velho apontando para hashes que nao existem mais.
				.setCacheControl(CacheControl.noCache())
				.resourceChain(false)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String caminho, Resource base) throws IOException {
						if (caminho.startsWith("api/")) {
							return null;
						}
						Resource arquivo = base.createRelative(caminho);
						// Barra "../" para fora da pasta.
						if (arquivo.isReadable() && checkResource(arquivo, base)) {
							return arquivo;
						}
						return indice.isReadable() ? indice : null;
					}
				});
	}
}
