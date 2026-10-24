package co.sierraalta.production.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Credenciales de inicio de sesión. Los límites de tamaño evitan procesar entradas desmedidas. */
public record LoginRequest(
		@NotBlank(message = "Ingrese el nombre de usuario.")
		@Size(max = 50, message = "El nombre de usuario no puede superar 50 caracteres.")
		String username,

		@NotBlank(message = "Ingrese la contraseña.")
		@Size(max = 72, message = "La contraseña no puede superar 72 caracteres.")
		String password) {

	@Override
	public String toString() {
		return "LoginRequest[username=" + username + ", password=***]";
	}
}
