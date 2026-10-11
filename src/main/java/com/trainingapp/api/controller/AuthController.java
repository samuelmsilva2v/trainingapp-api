package com.trainingapp.api.controller;

import com.trainingapp.api.config.AuthProperties;
import com.trainingapp.api.dto.AcessoResponse;
import com.trainingapp.api.dto.AuthConfigResponse;
import com.trainingapp.api.dto.CadastroRequest;
import com.trainingapp.api.dto.EmailRequest;
import com.trainingapp.api.dto.GoogleRequest;
import com.trainingapp.api.dto.LoginRequest;
import com.trainingapp.api.dto.RedefinirSenhaRequest;
import com.trainingapp.api.dto.TokenRequest;
import com.trainingapp.api.dto.TrocarSenhaRequest;
import com.trainingapp.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** O refresh token vai e volta so no cookie HttpOnly (path /api/auth); o corpo da resposta traz so o access token. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	static final String COOKIE = "refresh_token";

	private final AuthService auth;
	private final AuthProperties props;

	public AuthController(AuthService auth, AuthProperties props) {
		this.auth = auth;
		this.props = props;
	}

	@GetMapping("/config")
	public AuthConfigResponse config() {
		return new AuthConfigResponse(props.googleClientId() == null ? "" : props.googleClientId());
	}

	@PostMapping("/cadastro")
	@ResponseStatus(HttpStatus.CREATED)
	public void cadastrar(@Valid @RequestBody CadastroRequest request, HttpServletRequest http) {
		auth.cadastrar(request, http.getRemoteAddr());
	}

	@PostMapping("/confirmar-email")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void confirmarEmail(@Valid @RequestBody TokenRequest request) {
		auth.confirmarEmail(request.token());
	}

	@PostMapping("/login")
	public ResponseEntity<AcessoResponse> entrar(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
		return comCookie(auth.entrar(request, http.getRemoteAddr()));
	}

	@PostMapping("/google")
	public ResponseEntity<AcessoResponse> entrarComGoogle(@Valid @RequestBody GoogleRequest request) {
		return comCookie(auth.entrarComGoogle(request.credencial()));
	}

	@PostMapping("/refresh")
	public ResponseEntity<AcessoResponse> renovar(@CookieValue(name = COOKIE, required = false) String refresh) {
		AcessoResponse acesso = auth.renovar(refresh);
		// Reenvia o mesmo cookie com 1 ano novo: o navegador o apagaria no fim do prazo original.
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(refresh, AuthService.REFRESH_VALIDADE).toString()).body(acesso);
	}

	@PostMapping("/sair")
	public ResponseEntity<Void> sair(@CookieValue(name = COOKIE, required = false) String refresh) {
		auth.sair(refresh);
		return semCookie();
	}

	@PostMapping("/sair-todos")
	public ResponseEntity<Void> sairDeTodos() {
		auth.sairDeTodosOsAparelhos();
		return semCookie();
	}

	@PostMapping("/esqueci-senha")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void esqueciSenha(@Valid @RequestBody EmailRequest request, HttpServletRequest http) {
		auth.esqueciSenha(request.email(), http.getRemoteAddr());
	}

	@PostMapping("/redefinir-senha")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
		auth.redefinirSenha(request.token(), request.senha());
	}

	@PostMapping("/trocar-senha")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void trocarSenha(@Valid @RequestBody TrocarSenhaRequest request,
			@CookieValue(name = COOKIE, required = false) String refresh) {
		auth.trocarSenha(request.senhaAtual(), request.novaSenha(), refresh);
	}

	private ResponseEntity<AcessoResponse> comCookie(AuthService.Login login) {
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(login.refresh(), AuthService.REFRESH_VALIDADE).toString())
				.body(login.acesso());
	}

	private ResponseEntity<Void> semCookie() {
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
	}

	private ResponseCookie cookie(String valor, Duration idade) {
		return ResponseCookie.from(COOKIE, valor).httpOnly(true).secure(props.cookieSeguro()).sameSite("Strict")
				.path("/api/auth").maxAge(idade).build();
	}
}
