package com.trainingapp.api.service;

import com.trainingapp.api.config.AuthProperties;
import com.trainingapp.api.config.ErroDeAutenticacaoException;
import com.trainingapp.api.config.RegraDeNegocioException;
import com.trainingapp.api.dto.AcessoResponse;
import com.trainingapp.api.dto.CadastroRequest;
import com.trainingapp.api.dto.LoginRequest;
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
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Cadastro, login (senha e Google), sessao longa por refresh token e recuperacao de senha. A lista de e-mails
 * permitidos vale em todo ponto de entrada (cadastro, login, Google e renovacao). Falhas de autenticacao nao
 * desfazem a transacao, para o que ja foi apagado (token vencido) continuar apagado.
 */
@Service
@Transactional(noRollbackFor = ErroDeAutenticacaoException.class)
public class AuthService {

	/** Tempo sem usar o app ate o login vencer; cada renovacao recomeca a contagem. */
	public static final Duration REFRESH_VALIDADE = Duration.ofDays(365);

	static final Duration CONFIRMACAO_VALIDADE = Duration.ofHours(24);
	static final Duration REDEFINICAO_VALIDADE = Duration.ofHours(1);

	// O bcrypt so le os 72 primeiros bytes da senha.
	private static final int MAX_BYTES_SENHA = 72;

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
	private final PasswordEncoder encoder;
	private final AuthProperties props;
	private final Clock relogio;
	private final String urlBase;
	private final String hashFalso;

	public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens, TokenEmailRepository tokensEmail,
			TokenAcessoService tokenAcesso, FilaDeEmail fila, LimiteDeTentativas limite, VerificadorGoogle google,
			UsuarioAtual usuarioAtual, PasswordEncoder encoder, AuthProperties props, Clock relogio,
			@Value("${trainingapp.web.url-base}") String urlBase) {
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
		// Comparado quando o e-mail nao existe, para o tempo de resposta nao revelar quais e-mails tem conta.
		this.hashFalso = encoder.encode("senha-que-ninguem-usa");
	}

	// --- cadastro e confirmacao ---

	public void cadastrar(CadastroRequest r, String ip) {
		String chave = "cadastro:" + ip;
		limite.exigirLivre(chave);
		limite.registrar(chave);
		String email = normalizar(r.email());
		exigirPermitido(email);
		validarSenha(r.senha());
		if (usuarios.findByEmail(email).isPresent()) {
			throw new ErroDeAutenticacaoException(CONFLICT,
					"Já existe uma conta com este e-mail. Entre, ou use \"Esqueci a senha\".");
		}
		Usuario u = new Usuario();
		u.setId(UUID.randomUUID());
		u.setNome(r.nome().trim());
		u.setEmail(email);
		u.setSenhaHash(encoder.encode(r.senha()));
		// O id e atribuido aqui, entao o save faz merge e devolve a copia gerenciada; so ela pode ser referenciada.
		enviarLink(usuarios.save(u), TipoTokenEmail.CONFIRMACAO);
	}

	public void confirmarEmail(String token) {
		consumir(token, TipoTokenEmail.CONFIRMACAO).setEmailVerificado(true);
	}

	// --- login ---

	public Login entrar(LoginRequest r, String ip) {
		String email = normalizar(r.email());
		String chave = "login:" + email + "|" + ip;
		limite.exigirLivre(chave);
		Usuario u = usuarios.findByEmail(email).orElse(null);
		boolean temSenha = u != null && u.getSenhaHash() != null;
		boolean confere = senhaConfere(r.senha(), temSenha ? u.getSenhaHash() : hashFalso) && temSenha;
		if (!confere) {
			limite.registrar(chave);
			throw new ErroDeAutenticacaoException(UNAUTHORIZED, "E-mail ou senha incorretos");
		}
		if (!u.isEmailVerificado()) {
			throw new ErroDeAutenticacaoException(FORBIDDEN,
					"Confirme seu e-mail pelo link que enviamos. Perdeu o link? Use \"Esqueci a senha\".");
		}
		exigirPermitido(u.getEmail());
		limite.limpar(chave);
		return iniciarSessao(u);
	}

	public Login entrarComGoogle(String credencial) {
		VerificadorGoogle.Identidade g = google.verificar(credencial);
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
		return iniciarSessao(u);
	}

	// Mesmo e-mail de uma conta de senha: vira a mesma conta. Se aquele e-mail nunca foi confirmado, a senha foi
	// definida por alguem que nao provou ser o dono; descarta a senha em vez de entregar a conta a ela.
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
		return usuarios.save(u);
	}

	// --- sessao longa ---

	/** Troca o refresh token por um access token novo e empurra o vencimento do refresh. */
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

	/** O access token em uso continua valendo ate vencer (15 min); depois disso nenhum aparelho renova. */
	public void sairDeTodosOsAparelhos() {
		refreshTokens.deleteByUsuarioId(usuarioAtual.obter().getId());
	}

	// --- senha ---

	/** Responde igual exista a conta ou nao, para nao revelar quem tem cadastro. */
	public void esqueciSenha(String emailInformado, String ip) {
		String chave = "esqueci:" + ip;
		limite.exigirLivre(chave);
		limite.registrar(chave);
		usuarios.findByEmail(normalizar(emailInformado))
				.filter(u -> props.permitido(u.getEmail()))
				.ifPresent(u -> enviarLink(u, TipoTokenEmail.REDEFINICAO));
	}

	/** Receber o link prova que o e-mail e do usuario: confirma o e-mail, define a senha e derruba todos os aparelhos. */
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
		limite.exigirLivre(chave);
		if (u.getSenhaHash() == null) {
			throw new RegraDeNegocioException("Esta conta ainda não tem senha. Use \"Esqueci a senha\" para criar uma.");
		}
		if (!senhaConfere(senhaAtual, u.getSenhaHash())) {
			limite.registrar(chave);
			throw new RegraDeNegocioException("Senha atual incorreta");
		}
		validarSenha(novaSenha);
		limite.limpar(chave);
		u.setSenhaHash(encoder.encode(novaSenha));
		// Os outros aparelhos saem; este, que fez a troca, continua logado.
		RefreshToken atual = refreshAtual == null ? null
				: refreshTokens.findByTokenHash(Tokens.hash(refreshAtual)).filter(t -> t.getUsuario().getId().equals(u.getId())).orElse(null);
		if (atual == null) {
			refreshTokens.deleteByUsuarioId(u.getId());
		}
		else {
			refreshTokens.deleteByUsuarioIdAndIdNot(u.getId(), atual.getId());
		}
	}

	// --- apoio ---

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

	/** Link de uso unico: apaga o token ao usar e rejeita o vencido. */
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
		if (senha.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_SENHA) {
			throw new RegraDeNegocioException("A senha pode ter no máximo 72 bytes (menos caracteres se tiver acentos)");
		}
	}

	private boolean senhaConfere(String senha, String hash) {
		return senha.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES_SENHA && encoder.matches(senha, hash);
	}

	private static String normalizar(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
