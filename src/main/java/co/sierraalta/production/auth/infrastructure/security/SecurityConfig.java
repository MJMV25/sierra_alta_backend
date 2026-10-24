package co.sierraalta.production.auth.infrastructure.security;

import static co.sierraalta.production.auth.domain.Permission.FORECAST_EXECUTE;
import static co.sierraalta.production.auth.domain.Permission.FORECAST_READ;
import static co.sierraalta.production.auth.domain.Permission.HISTORY_READ;
import static co.sierraalta.production.auth.domain.Permission.HISTORY_WRITE;
import static co.sierraalta.production.auth.domain.Permission.INDICATORS_READ;
import static co.sierraalta.production.auth.domain.Permission.USERS_MANAGE;

import java.util.List;

import co.sierraalta.production.auth.application.SecurityEventRecorder;
import co.sierraalta.production.auth.domain.SecurityEventResult;
import co.sierraalta.production.auth.domain.SecurityEventType;
import co.sierraalta.production.auth.infrastructure.persistence.UserAccountRepository;
import co.sierraalta.production.shared.web.ApiPaths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;

/**
 * Autenticación central y autorización en el servidor.
 *
 * <ul>
 * <li>Sesión HTTP con cookie {@code HttpOnly}; el frontend nunca maneja la credencial de sesión.</li>
 * <li>CSRF activo: el token viaja en la cookie legible {@code XSRF-TOKEN} y React lo reenvía en la
 * cabecera {@code X-XSRF-TOKEN} en toda solicitud que modifica estado (incluido el login).</li>
 * <li>Contraseñas con hash adaptativo (bcrypt por defecto, con prefijo para migrar de algoritmo).</li>
 * <li>Autorización por permiso en una única tabla de reglas y denegación por defecto: un endpoint
 * nuevo no queda expuesto hasta que se le asigna un permiso aquí.</li>
 * <li>Seguridad por método ({@code @PreAuthorize}) en los casos de uso: segunda verificación del
 * permiso, independiente de la ruta por la que se invoquen.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	static final String SESSION_COOKIE = "JSESSIONID";

	private static final String API = ApiPaths.V1;

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http, CsrfTokenRepository csrfTokenRepository,
			CsrfTokenRequestHandler csrfTokenRequestHandler, SecurityContextRepository securityContextRepository,
			ProblemDetailsSecurityHandler problemHandler, UserAccountRepository users,
			SecurityEventRecorder eventRecorder) throws Exception {
		http
			.cors(Customizer.withDefaults())
			.csrf(csrf -> csrf
				.csrfTokenRepository(csrfTokenRepository)
				.csrfTokenRequestHandler(csrfTokenRequestHandler))
			.securityContext(context -> context.securityContextRepository(securityContextRepository))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.logout(logout -> logout
				.logoutUrl(API + "/auth/logout")
				.addLogoutHandler(auditLogout(eventRecorder))
				.deleteCookies(SESSION_COOKIE)
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(problemHandler)
				.accessDeniedHandler(problemHandler))
			.addFilterBefore(new AccountStatusFilter(users, securityContextRepository), AuthorizationFilter.class)
			.authorizeHttpRequests(auth -> auth
				// Público: obtener token CSRF, iniciar sesión, salud del servicio y página de error.
				.requestMatchers(HttpMethod.GET, API + "/auth/csrf").permitAll()
				.requestMatchers(HttpMethod.POST, API + "/auth/login").permitAll()
				.requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()

				// Cualquier sesión válida.
				.requestMatchers(HttpMethod.GET, API + "/auth/me").authenticated()

				// auth: administración de cuentas.
				.requestMatchers(API + "/admin/**").hasAuthority(USERS_MANAGE.authority())

				// history: lectura y carga de históricos.
				.requestMatchers(HttpMethod.GET, API + "/history/**").hasAuthority(HISTORY_READ.authority())
				.requestMatchers(HttpMethod.POST, API + "/history/**").hasAuthority(HISTORY_WRITE.authority())
				.requestMatchers(HttpMethod.PUT, API + "/history/**").hasAuthority(HISTORY_WRITE.authority())
				.requestMatchers(HttpMethod.PATCH, API + "/history/**").hasAuthority(HISTORY_WRITE.authority())

				// forecast: ejecutar y consultar pronósticos.
				.requestMatchers(HttpMethod.POST, API + "/forecasts/**").hasAuthority(FORECAST_EXECUTE.authority())
				.requestMatchers(HttpMethod.GET, API + "/forecasts/**").hasAuthority(FORECAST_READ.authority())

				// indicators: consultar MAD y MAPE.
				.requestMatchers(HttpMethod.GET, API + "/indicators/**").hasAuthority(INDICATORS_READ.authority())

				// Privilegio mínimo: todo lo no declarado se rechaza.
				.anyRequest().denyAll());
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AccountUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder, ApplicationEventPublisher eventPublisher) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		ProviderManager manager = new ProviderManager(provider);
		manager.setAuthenticationEventPublisher(new DefaultAuthenticationEventPublisher(eventPublisher));
		return manager;
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
				new HttpSessionSecurityContextRepository());
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository(
			@Value("${server.servlet.session.cookie.secure:false}") boolean secureCookies) {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> cookie.sameSite("Lax").secure(secureCookies));
		return repository;
	}

	/** El SPA envía el valor crudo de la cookie en la cabecera; no se usa codificación XOR. */
	@Bean
	CsrfTokenRequestHandler csrfTokenRequestHandler() {
		return new CsrfTokenRequestAttributeHandler();
	}

	/** Al iniciar sesión: nuevo identificador de sesión (anti fijación) y nuevo token CSRF. */
	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository,
			CsrfTokenRequestHandler csrfTokenRequestHandler) {
		CsrfAuthenticationStrategy csrfStrategy = new CsrfAuthenticationStrategy(csrfTokenRepository);
		csrfStrategy.setRequestHandler(csrfTokenRequestHandler);
		return new CompositeSessionAuthenticationStrategy(
				List.of(new ChangeSessionIdAuthenticationStrategy(), csrfStrategy));
	}

	private static LogoutHandler auditLogout(SecurityEventRecorder eventRecorder) {
		return (request, response, authentication) -> {
			if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
				eventRecorder.record(principal.id(), SecurityEventType.LOGOUT, SecurityEventResult.SUCCESS);
			}
		};
	}
}
