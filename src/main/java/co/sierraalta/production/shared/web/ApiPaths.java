package co.sierraalta.production.shared.web;

/**
 * Prefijo versionado de la API REST. Un cambio incompatible del contrato se publica bajo una nueva
 * versión ({@code /api/v2}) en lugar de modificar la vigente.
 */
public final class ApiPaths {

	public static final String V1 = "/api/v1";

	private ApiPaths() {
	}
}
