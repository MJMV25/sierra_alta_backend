package co.sierraalta.production.shared.error;

import java.io.Serial;

/**
 * Operación cuyo contrato y permisos ya están definidos, pero cuya lógica aún no se implementa
 * porque sus reglas están pendientes de validación con Ingeniería Industrial. HTTP 501.
 *
 * <p>Permite probar la autorización por rol de extremo a extremo sin simular resultados falsos.
 */
public class FeatureNotImplementedException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public FeatureNotImplementedException(String feature) {
		super("Funcionalidad aún no implementada: " + feature);
	}
}
