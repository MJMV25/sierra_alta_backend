package co.sierraalta.production.auth.infrastructure.security;

import co.sierraalta.production.auth.application.SecurityEventRecorder;
import co.sierraalta.production.auth.domain.SecurityEventResult;
import co.sierraalta.production.auth.domain.SecurityEventType;
import co.sierraalta.production.auth.domain.UserAccount;
import co.sierraalta.production.auth.domain.Usernames;
import co.sierraalta.production.auth.infrastructure.persistence.UserAccountRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Audita los intentos de inicio de sesión publicados por el {@code AuthenticationManager}. */
@Component
public class AuthenticationAuditListener {

	private final SecurityEventRecorder eventRecorder;
	private final UserAccountRepository users;

	public AuthenticationAuditListener(SecurityEventRecorder eventRecorder, UserAccountRepository users) {
		this.eventRecorder = eventRecorder;
		this.users = users;
	}

	@EventListener
	public void onSuccess(AuthenticationSuccessEvent event) {
		if (event.getAuthentication().getPrincipal() instanceof AccountPrincipal principal) {
			eventRecorder.record(principal.id(), SecurityEventType.LOGIN, SecurityEventResult.SUCCESS);
		}
	}

	/** Si el nombre corresponde a una cuenta existente se asocia; si no, el evento queda anónimo. */
	@EventListener
	public void onFailure(AbstractAuthenticationFailureEvent event) {
		Long userId = users.findByUsername(Usernames.normalize(event.getAuthentication().getName()))
				.map(UserAccount::getId)
				.orElse(null);
		eventRecorder.record(userId, SecurityEventType.LOGIN, SecurityEventResult.FAILURE);
	}
}
