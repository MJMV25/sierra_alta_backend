package co.sierraalta.production.auth.api.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull(message = "Indique si la cuenta queda activa.") Boolean active) {
}
