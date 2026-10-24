package co.sierraalta.production;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada del monolito modular.
 *
 * <p>Módulos: {@code auth} (identidad, sesión y permisos), {@code history} (productos, series y
 * observaciones), {@code forecast} (coordinación de ejecuciones) e {@code indicators} (MAD y MAPE).
 * Los módulos colaboran mediante llamadas internas; {@code shared} contiene solo piezas transversales.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SierraAltaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SierraAltaBackendApplication.class, args);
	}
}
