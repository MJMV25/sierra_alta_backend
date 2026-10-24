package co.sierraalta.production.auth.api.dto;

import co.sierraalta.production.auth.domain.RoleCode;
import co.sierraalta.production.auth.domain.Usernames;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de una cuenta administrada. El máximo de 72 caracteres corresponde al límite de bcrypt.
 * La política definitiva de contraseñas debe acordarse antes de operar con usuarios reales.
 */
public record CreateUserRequest(
		@NotBlank(message = "El nombre de usuario es obligatorio.")
		@Pattern(regexp = Usernames.PATTERN,
				message = "Use entre 3 y 50 caracteres: letras, números, punto, guion o guion bajo.")
		String username,

		@NotBlank(message = "La contraseña es obligatoria.")
		@Size(min = 10, max = 72, message = "La contraseña debe tener entre 10 y 72 caracteres.")
		String password,

		@NotNull(message = "El rol es obligatorio.")
		RoleCode role) {

	@Override
	public String toString() {
		return "CreateUserRequest[username=" + username + ", password=***, role=" + role + "]";
	}
}
