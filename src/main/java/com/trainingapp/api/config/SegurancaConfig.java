package com.trainingapp.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(AuthProperties.class)
public class SegurancaConfig {

	// Sem sessao nem CSRF: a API so aceita Bearer. O cookie do refresh (SameSite=Strict, POST) so serve em /api/auth.
	@Bean
	SecurityFilterChain filtros(HttpSecurity http, @Value("${trainingapp.cors.origens:https://localhost}") List<String> origens)
			throws Exception {
		http.csrf(csrf -> csrf.disable())
				.cors(c -> c.configurationSource(cors(origens)))
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(a -> a
						.requestMatchers("/api/auth/sair-todos", "/api/auth/trocar-senha", "/api/auth/excluir-conta").authenticated()
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers("/api/**").authenticated()
						// Front estatico (perfil online), health e o resto.
						.anyRequest().permitAll())
				.oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
		return http.build();
	}

	/**
	 * CORS so para as origens listadas (o app Android roda em https://localhost). Qualquer outra origem nao recebe
	 * configuracao e segue como antes: a web, na mesma origem, nunca depende de o proxy repassar host e esquema certos.
	 * Sem credenciais: o app usa Bearer e o refresh no corpo, nunca o cookie.
	 */
	static CorsConfigurationSource cors(List<String> origens) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(origens);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Cliente"));
		return request -> request.getRequestURI().startsWith("/api/") && origens.contains(request.getHeader(HttpHeaders.ORIGIN))
				? config : null;
	}

	@Bean
	SecretKey chaveJwt(AuthProperties props) {
		String segredo = props.jwtSegredo();
		if (segredo == null || segredo.length() < 32) {
			throw new IllegalStateException("Defina trainingapp.auth.jwt-segredo (TRAININGAPP_AUTH_JWT_SEGREDO) com 32+ caracteres");
		}
		return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey chave) {
		return NimbusJwtEncoder.withSecretKey(chave).build();
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey chave) {
		return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
