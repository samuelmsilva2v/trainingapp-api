package com.trainingapp.api.service;

import com.trainingapp.api.config.AuthProperties;
import com.trainingapp.api.config.ErroDeAutenticacaoException;
import com.trainingapp.api.config.RegraDeNegocioException;
import com.trainingapp.api.dto.AcessoResponse;
import com.trainingapp.api.dto.CadastroRequest;
import com.trainingapp.api.dto.LoginRequest;
import com.trainingapp.api.model.FotoTipo;
import com.trainingapp.api.model.RefreshToken;
import com.trainingapp.api.model.TipoTokenEmail;
import com.trainingapp.api.model.TokenEmail;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.RefreshTokenRepository;
import com.trainingapp.api.repository.TokenEmailRepository;
import com.trainingapp.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@Transactional(noRollbackFor = ErroDeAutenticacaoException.class)
public class AuthService {

	public static final Duration REFRESH_VALIDADE = Duration.ofDays(365);

	static final Duration CONFIRMACAO_VALIDADE = Duration.ofHours(24);
	static final Duration REDEFINICAO_VALIDADE = Duration.ofHours(1);
	public static final int LIMITE_POR_IP = 30;

	public record Login(AcessoResponse acesso, String refresh) {
	}

	private final UsuarioRepository usuarios;
	private final RefreshTokenRepository refreshTokens;
	private final TokenEmailRepository tokensEmail;
	private final TokenAcessoService tokenAcesso;
	private final FilaDeEmail fila;
	private final LimiteDeTentativas limite;
	private final VerificadorGoogle google;
	private final UsuarioAtual usuarioAtual;
	private final ExclusaoDeConta exclusao;
	private final PasswordEncoder encoder;
	private final AuthProperties props;
	private final Clock relogio;
	private final String urlBase;
	private final String hashFalso;

	public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens, TokenEmailRepository tokensEmail,
			TokenAcessoService tokenAcesso, FilaDeEmail fila, LimiteDeTentativas limite, VerificadorGoogle google,
			UsuarioAtual usuarioAtual, ExclusaoDeConta exclusao, PasswordEncoder encoder, AuthProperties props, Clock relogio,
			@Value("${trainingapp.web.url-base}") String urlBase) {
		this.exclusao = exclusao;
		this.usuarios = usuarios;
		this.refreshTokens = refreshTokens;
		this.tokensEmail = tokensEmail;
		this.tokenAcesso = tokenAcesso;
		this.fila = fila;
		this.limite = limite;
		this.google = google;
		this.usuarioAtual = usuarioAtual;
		this.encoder = encoder;
		this.props = props;
		this.relogio = relogio;
		this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
		// Comparado quando o e-mail nao existe, para o tempo de resposta nao revelar quem tem conta.
		this.hashFalso = encoder.encode("senha-que-ninguem-usa");
	}

	public void cadastrar(CadastroRequest r, String ip) {
		limite.tentar("cadastro:" + ip);
		String email = normalizar(r.email());
		exigirPermitido(email);
		validarSenha(r.senha());
		String senhaHash = encoder.encode(r.senha());
		// Responde igual exista a conta ou nao (sem 409), para nao revelar quem tem cadastro.
		Usuario existente = usuarios.findByEmail(email).orElse(null);
		if (existente != null) {
			fila.enfileirar(email, "Você já tem conta no TrainingApp", ("Olá, %s!%n%nAlguém tentou criar uma conta no TrainingApp "
					+ "com este e-mail, mas você já tem uma. Entre com seu e-mail e senha ou com o Google; se não lembra a senha, "
					+ "use \"Esqueci a senha\" na tela de entrada:%n%n%s/entrar%n%n"
					+ "Se não foi você, ignore este e-mail.").formatted(existente.getNome(), urlBase));
			return;
		}
		Usuario u = new Usuario();
		u.setId(UUID.randomUUID());
		u.setNome(r.nome().trim());
		u.setEmail(email);
		u.setSenhaHash(senhaHash);
		// Com o id ja atribuido o save faz merge: so a copia devolvida e gerenciada.
		enviarLink(usuarios.save(u), TipoTokenEmail.CONFIRMACAO);
	}

	public void confirmarEmail(String token) {
		consumir(token, TipoTokenEmail.CONFIRMACAO).setEmailVerificado(true);
	}

	public Login entrar(LoginRequest r, String ip) {
		String email = normalizar(r.email());
		String chave = "login:" + email + "|" + ip;
		limite.tentar("login-ip:" + ip, LIMITE_POR_IP);
		limite.tentar(chave);
		Usuario u = usuarios.findByEmail(email).orElse(null);
		boolean temSenha = u != null && u.getSenhaHash() != null;
		boolean confere = senhaConfere(r.senha(), temSenha ? u.getSenhaHash() : hashFalso) && temSenha;
		if (!confere) {
			throw new ErroDeAutenticacaoException(UNAUTHORIZED, "E-mail ou senha incorretos");
		}
		limite.limpar(chave);
		if (!u.isEmailVerificado()) {
			throw new ErroDeAutenticacaoException(FORBIDDEN,
					"Confirme seu e-mail pelo link que enviamos. Perdeu o link? Use \"Esqueci a senha\".");
		}
		exigirPermitido(u.getEmail());
		return iniciarSessao(u);
	}

	public Login entrarComGoogle(String codigo, String ip) {
		limite.tentar("google:" + ip, LIMITE_POR_IP);
		return entrarComo(google.verificarCodigo(codigo));
	}

	public Login entrarComGoogleIdToken(String idToken, String ip) {
		limite.tentar("google:" + ip, LIMITE_POR_IP);
		return entrarComo(google.verificarIdToken(idToken));
	}

	private Login entrarComo(VerificadorGoogle.Identidade g) {
		if (!g.emailVerificado()) {
			throw new ErroDeAutenticacaoException(FORBIDDEN, "O e-mail desta conta Google não está verificado");
		}
		String email = normalizar(g.email());
		Usuario u = usuarios.findByGoogleSub(g.sub()).orElse(null);
		if (u == null) {
			exigirPermitido(email);
			u = usuarios.findByEmail(email).map(existente -> vincularGoogle(existente, g.sub()))
					.orElseGet(() -> criarComGoogle(g, email));
		}
		exigirPermitido(u.getEmail());
		u.setFotoGoogleUrl(g.foto());
		return iniciarSessao(u);
	}

	// Senha de e-mail nunca confirmado foi definida por quem nao provou ser o dono: e descartada.
	private Usuario vincularGoogle(Usuario u, String sub) {
		if (u.getGoogleSub() != null) {
			throw new ErroDeAutenticacaoException(FORBIDDEN, "Este e-mail já está ligado a outra conta Google");
		}
		if (!u.isEmailVerificado()) {
			u.setSenhaHash(null);
			u.setEmailVerificado(true);
			tokensEmail.deleteByUsuarioIdAndTipo(u.getId(), TipoTokenEmail.CONFIRMACAO);
		}
		u.setGoogleSub(sub);
		if (u.getFotoTipo() == FotoTipo.PADRAO) u.setFotoTipo(FotoTipo.GOOGLE);
		return u;
	}

	private Usuario criarComGoogle(VerificadorGoogle.Identidade g, String email) {
		String nome = g.nome() == null || g.nome().isBlank() ? email.substring(0, email.indexOf('@')) : g.nome().trim();
		Usuario u = new Usuario();
		u.setId(UUID.randomUUID());
		u.setNome(nome.substring(0, Math.min(nome.length(), 100)));
		u.setEmail(email);
		u.setGoogleSub(g.sub());
		u.setEmailVerificado(true);
		u.setFotoTipo(FotoTipo.GOOGLE);
		return usuarios.save(u);
	}

	public AcessoResponse renovar(String refresh) {
		if (refresh == null || refresh.isBlank()) {
			throw new ErroDeAutenticacaoException(UNAUTHORIZED, "Sessão expirada");
		}
		RefreshToken r = refreshTokens.findByTokenHash(Tokens.hash(refresh))
				.orElseThrow(() -> new ErroDeAutenticacaoException(UNAUTHORIZED, "Sessão expirada"));
		Instant agora = Instant.now(relogio);
		if (r.getUltimoUsoEm().plus(REFRESH_VALIDADE).isBefore(agora)) {
			refreshTokens.delete(r);
			throw new ErroDeAutenticacaoException(UNAUTHORIZED, "Sessão expirada");
		}
		Usuario u = r.getUsuario();
		exigirPermitido(u.getEmail());
		r.setUltimoUsoEm(agora);
		return tokenAcesso.gerar(u.getId());
	}

	public void sair(String refresh) {
		if (refresh != null) {
			refreshTokens.findByTokenHash(Tokens.hash(refresh)).ifPresent(refreshTokens::delete);
		}
	}

	public void sairDeTodosOsAparelhos() {
		refreshTokens.deleteByUsuarioId(usuarioAtual.obter().getId());
	}

	public void esqueciSenha(String emailInformado, String ip) {
		limite.tentar("esqueci:" + ip);
		usuarios.findByEmail(normalizar(emailInformado))
				.filter(u -> props.permitido(u.getEmail()))
				.ifPresent(u -> enviarLink(u, TipoTokenEmail.REDEFINICAO));
	}

	public void redefinirSenha(String token, String novaSenha) {
		validarSenha(novaSenha);
		Usuario u = consumir(token, TipoTokenEmail.REDEFINICAO);
		u.setSenhaHash(encoder.encode(novaSenha));
		u.setEmailVerificado(true);
		refreshTokens.deleteByUsuarioId(u.getId());
	}

	public void trocarSenha(String senhaAtual, String novaSenha, String refreshAtual) {
		Usuario u = usuarioAtual.obter();
		String chave = "trocar:" + u.getId();
		limite.tentar(chave);
		if (u.getSenhaHash() == null) {
			throw new RegraDeNegocioException("Esta conta ainda não tem senha. Use \"Esqueci a senha\" para criar uma.");
		}
		if (!senhaConfere(senhaAtual, u.getSenhaHash())) {
			throw new RegraDeNegocioException("Senha atual incorreta");
		}
		limite.limpar(chave);
		validarSenha(novaSenha);
		u.setSenhaHash(encoder.encode(novaSenha));
		RefreshToken atual = refreshAtual == null ? null
				: refreshTokens.findByTokenHash(Tokens.hash(refreshAtual)).filter(t -> t.getUsuario().getId().equals(u.getId())).orElse(null);
		if (atual == null) {
			refreshTokens.deleteByUsuarioId(u.getId());
		}
		else {
			refreshTokens.deleteByUsuarioIdAndIdNot(u.getId(), atual.getId());
		}
	}

	// Exige senha ou login Google novo: um access token roubado nao pode bastar para apagar a conta.
	public void excluirConta(String senha, String codigo, String idToken) {
		Usuario u = usuarioAtual.obter();
		String chave = "excluir:" + u.getId();
		limite.tentar(chave);
		if (senha != null && !senha.isBlank()) {
			if (u.getSenhaHash() == null || !senhaConfere(senha, u.getSenhaHash())) {
				throw new RegraDeNegocioException("Senha incorreta");
			}
		}
		else if ((codigo != null && !codigo.isBlank()) || (idToken != null && !idToken.isBlank())) {
			VerificadorGoogle.Identidade g = codigo != null && !codigo.isBlank() ? google.verificarCodigo(codigo)
					: google.verificarIdToken(idToken);
			if (u.getGoogleSub() == null || !u.getGoogleSub().equals(g.sub())) {
				throw new ErroDeAutenticacaoException(FORBIDDEN, "Esta não é a conta Google ligada à sua conta");
			}
		}
		else {
			throw new RegraDeNegocioException("Confirme com a sua senha ou com a sua conta Google");
		}
		limite.limpar(chave);
		exclusao.apagar(u);
	}

	private Login iniciarSessao(Usuario u) {
		String refresh = Tokens.novo();
		RefreshToken r = new RefreshToken();
		r.setUsuario(u);
		r.setTokenHash(Tokens.hash(refresh));
		r.setUltimoUsoEm(Instant.now(relogio));
		refreshTokens.save(r);
		return new Login(tokenAcesso.gerar(u.getId()), refresh);
	}

	private void enviarLink(Usuario u, TipoTokenEmail tipo) {
		tokensEmail.deleteByUsuarioIdAndTipo(u.getId(), tipo);
		String token = Tokens.novo();
		TokenEmail t = new TokenEmail();
		t.setUsuario(u);
		t.setTipo(tipo);
		t.setTokenHash(Tokens.hash(token));
		boolean confirmacao = tipo == TipoTokenEmail.CONFIRMACAO;
		t.setExpiraEm(Instant.now(relogio).plus(confirmacao ? CONFIRMACAO_VALIDADE : REDEFINICAO_VALIDADE));
		tokensEmail.save(t);
		String link = urlBase + (confirmacao ? "/confirmar-email" : "/redefinir-senha") + "?token=" + token;
		if (confirmacao) {
			fila.enfileirar(u.getEmail(), "Confirme seu e-mail no TrainingApp", ("Olá, %s!%n%nSua conta no TrainingApp foi criada. "
					+ "Para ativá-la, confirme seu e-mail pelo link abaixo (vale por 24 horas):%n%n%s%n%n"
					+ "Se você não criou esta conta, ignore este e-mail.").formatted(u.getNome(), link));
		}
		else {
			fila.enfileirar(u.getEmail(), "Redefinir sua senha do TrainingApp", ("Olá, %s!%n%nRecebemos um pedido para redefinir "
					+ "sua senha. Use o link abaixo (vale por 1 hora):%n%n%s%n%n"
					+ "Se não foi você, ignore este e-mail: sua senha continua a mesma.").formatted(u.getNome(), link));
		}
	}

	private Usuario consumir(String token, TipoTokenEmail tipo) {
		ErroDeAutenticacaoException invalido = new ErroDeAutenticacaoException(BAD_REQUEST, "Link inválido ou expirado");
		TokenEmail t = tokensEmail.findByTokenHashAndTipo(Tokens.hash(token), tipo).orElseThrow(() -> invalido);
		tokensEmail.delete(t);
		if (t.getExpiraEm().isBefore(Instant.now(relogio))) {
			throw invalido;
		}
		return t.getUsuario();
	}

	private void exigirPermitido(String email) {
		if (!props.permitido(email)) {
			throw new ErroDeAutenticacaoException(HttpStatus.FORBIDDEN, "Acesso restrito: este e-mail não está autorizado a usar o app");
		}
	}

	private void validarSenha(String senha) {
		String problema = RegrasDeSenha.problema(senha);
		if (problema != null) {
			throw new RegraDeNegocioException(problema);
		}
	}

	private boolean senhaConfere(String senha, String hash) {
		return senha.getBytes(StandardCharsets.UTF_8).length <= RegrasDeSenha.MAXIMO_BYTES && encoder.matches(senha, hash);
	}

	private static String normalizar(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
