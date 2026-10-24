# ADR 02 — Separar frontend React y backend Spring Boot

**Estado:** propuesto

**Contexto.** Se necesita una interfaz React y un login funcional.

**Alternativas.** Vistas servidas por el backend · frontend independiente con API.

**Decisión.** React + Vite para el frontend; Spring Boot expone una API REST JSON. Spring Security
autentica y autoriza por rol con **sesión HTTP en cookie `HttpOnly`** (no tokens en `localStorage`)
y protección **CSRF** por doble envío (`XSRF-TOKEN` / `X-XSRF-TOKEN`). HTTPS en despliegue.

**Consecuencias.**
- (+) La lógica de negocio y la seguridad permanecen en el servidor.
- (+) El token de sesión no es accesible desde JavaScript (mitiga robo por XSS).
- (−) Dos construcciones; CORS con credenciales debe listar orígenes exactos (`CORS_ALLOWED_ORIGINS`).
- (−) El frontend debe enviar el token CSRF y releerlo tras el login.
- Ocultar opciones en React no acredita seguridad: lo verifican las pruebas de acceso directo por rol.
