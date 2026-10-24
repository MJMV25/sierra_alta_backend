package co.sierraalta.production.auth.api.dto;

import co.sierraalta.production.auth.domain.RoleCode;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull(message = "El rol es obligatorio.") RoleCode role) {
}
