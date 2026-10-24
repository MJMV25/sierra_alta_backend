/**
 * Módulo de autenticación y roles: identidad, estado de cuenta, sesión, permisos y auditoría de acceso.
 *
 * <ul>
 * <li>{@code domain}: rol, permisos por rol, cuenta y evento de seguridad.</li>
 * <li>{@code application}: casos de uso de administración de cuentas y registro de eventos.</li>
 * <li>{@code infrastructure.security}: Spring Security (sesión, CSRF, reglas de autorización).</li>
 * <li>{@code infrastructure.persistence}: repositorios Spring Data sobre {@code rol}, {@code usuario} y {@code evento}.</li>
 * <li>{@code api}: endpoints REST y DTO.</li>
 * </ul>
 * Protege los accesos a los demás módulos; estos no dependen de sus clases internas.
 */
package co.sierraalta.production.auth;
