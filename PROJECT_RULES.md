# Reglas de desarrollo — Programación con Tecnologías Web

> Aplicables al proyecto web. Basadas en las clases 3, 4, 5, 8, 9 y 10 proporcionadas. Las normas sobre testing, Git, TypeScript y despliegue son recomendaciones adicionales, no contenidos atribuidos literalmente a las diapositivas.

## 1. Principios generales
- **R01**. Mantener el código simple, legible, cohesivo y con responsabilidades explícitas.
- **R02**. Evitar duplicación, funciones gigantes, código muerto y abstracciones innecesarias.
- **R03**. Nombrar variables, funciones, módulos y archivos de forma descriptiva y consistente.
- **R04**. No cambiar comportamientos existentes ajenos al requerimiento; hacer cambios mínimos, localizados y verificables.
- **R05**. Documentar decisiones técnicas relevantes y los contratos públicos.

## 2. Arquitectura backend — clase 8
- **R06**. Organizar el código por módulos de negocio; dentro de cada módulo separar routes, controllers, services y repositories cuando aplique.
- **R07**. Las rutas solo declaran endpoints, conectan controladores y middlewares.
- **R08**. Los controladores interpretan la petición, llaman servicios y construyen respuestas HTTP; no contienen reglas del negocio ni SQL.
- **R09**. Los servicios implementan casos de uso, invariantes y reglas de negocio; no dependen de objetos HTTP (`req`/`res`).
- **R10**. Los repositorios encapsulan la persistencia y las consultas a la base de datos; no construyen respuestas HTTP.
- **R11**. Centralizar autenticación, autorización, validación y registro de solicitudes en middlewares apropiados; las verificaciones de autorización por recurso también se aplican en el caso de uso.
- **R12**. Mantener dependencias claras y evitar importaciones circulares entre módulos.

### Organización orientativa
```text
src/
  modules/
    users/
      users.routes.ts
      users.controller.ts
      users.service.ts
      users.repository.ts
      users.schema.ts
    inventory/
      inventory.routes.ts
      inventory.controller.ts
      inventory.service.ts
      inventory.repository.ts
      inventory.schema.ts
  middlewares/
    authenticate.ts
    authorize.ts
    validate.ts
    error-handler.ts
  config/
  shared/
  app.ts
```
Esta es una estructura orientativa, no una imposición de lenguaje, framework o nombres de módulos.

## 3. API y HTTP — clase 3
- **R13**. Tratar la API como contrato explícito: ruta, método, parámetros, cuerpo, respuesta y posibles errores.
- **R14**. Usar recursos con URLs consistentes (`/api/v1/products`, `/api/v1/products/:id`).
- **R15**. Usar métodos HTTP según la intención: GET leer, POST crear/acciones, PUT reemplazar, PATCH actualizar parcialmente, DELETE eliminar.
- **R16**. Respetar semántica HTTP: 200 consulta exitosa; 201 creación; 204 éxito sin cuerpo; 400 petición inválida; 401 falta autenticación válida; 403 falta permiso; 404 inexistente; 409 conflicto; 422 datos semánticamente inválidos; 500 error inesperado.
- **R17**. Devolver JSON coherente, sin filtrar detalles internos en mensajes de error.
- **R18**. Validar parámetros de ruta, query y cuerpo del lado servidor antes de ejecutar el caso de uso.
- **R19**. No asumir que HTTP conserva sesión entre solicitudes; establecer autenticación explícita.

## 4. Datos y persistencia — clase 4
- **R20**. Elegir SQL o NoSQL según el dominio, estructura, relaciones, consistencia, patrón de uso y escala; no por popularidad.
- **R21**. Favorecer bases relacionales cuando haya entidades relacionadas, operaciones transaccionales y consistencia crítica; considerar modelos documentales cuando la flexibilidad de esquema tenga justificación.
- **R22**. Definir claves primarias, relaciones, restricciones y políticas de integridad para esquemas relacionales.
- **R23**. Usar transacciones cuando una operación de negocio necesite actualizar múltiples registros de forma atómica.
- **R24**. No duplicar datos sin una razón clara; documentar cualquier desnormalización intencional.
- **R25**. Mantener migraciones y cambios de esquema reproducibles; seleccionar índices según consultas reales.
- **R26**. Evitar consultas a base de datos fuera de repositorios o adaptadores de persistencia.

## 5. Usabilidad — clase 5
- **R27**. Dar feedback visible e inmediato a cada acción: cargando, éxito, error y estados vacíos.
- **R28**. Deshabilitar temporalmente acciones que puedan duplicarse mientras se procesa una operación, sin sustituir mecanismos de idempotencia del servidor cuando sean necesarios.
- **R29**. Usar lenguaje del usuario y seguir el flujo real de su tarea, no la estructura interna de la base de datos.
- **R30**. Mantener etiquetas, botones, iconos y comportamiento consistentes entre pantallas.
- **R31**. Prevenir errores con controles adecuados, límites claros, validación temprana y confirmaciones en acciones destructivas.
- **R32**. Ofrecer cancelar, volver o revertir operaciones cuando sea técnica y funcionalmente viable.
- **R33**. Minimizar carga cognitiva y mostrar opciones reconocibles; priorizar información relevante.
- **R34**. Mostrar errores comprensibles: qué ocurrió, qué debe corregirse y cómo continuar; evitar mostrar solo códigos HTTP.
- **R35**. Diseñar para teclado, foco visible, etiquetas accesibles y diferentes tamaños de pantalla.

## 6. JWT y autenticación — clase 9
- **R36**. Verificar siempre la firma, algoritmo esperado, expiración y claims relevantes del JWT en el servidor.
- **R37**. No almacenar contraseñas, hashes, información financiera ni datos clínicos en payloads JWT: Base64URL no cifra el contenido.
- **R38**. Usar access tokens con duración limitada y refresh tokens separados con revocación y rotación cuando el caso lo requiera.
- **R39**. Implementar un flujo claro de login, renovación y logout; revocar las credenciales renovables durante el cierre de sesión.
- **R40**. Mantener secretos y claves fuertes fuera del código y del repositorio; emplear variables de entorno o un gestor de secretos.
- **R41**. Evitar guardar tokens sensibles en `localStorage` cuando existan opciones más seguras; si se usan cookies `HttpOnly`, configurar `Secure`, `SameSite` y protección CSRF según el flujo.
- **R42**. Distinguir autenticación (identidad, 401) y autorización (permisos, 403). No confiar en roles enviados libremente por el cliente.

## 7. Seguridad — clase 10
- **R43**. Nunca concatenar entradas de usuario para formar consultas SQL; utilizar consultas parametrizadas o un ORM seguro.
- **R44**. Escapar/renderizar texto no confiable de forma segura; evitar `innerHTML` y `dangerouslySetInnerHTML` con entrada no confiable.
- **R45**. Tratar datos recibidos por formularios, rutas, headers y servicios externos como no confiables; validar tipo, formato, tamaño y reglas de dominio.
- **R46**. Si se autentica con cookies automáticas, aplicar medidas adecuadas contra CSRF. Los JWT en `Authorization` reducen ese vector específico, pero no eliminan otras vulnerabilidades.
- **R47**. Retornar solo campos expresamente permitidos (DTO o whitelist); nunca devolver entidades completas con contraseñas, hashes, tokens, notas internas o metadatos innecesarios.
- **R48**. No exponer stack traces, credenciales ni información sensible en respuestas o logs.
- **R49**. Configurar permisos mínimos para cuentas de base de datos y servicios; revisar controles de acceso por recurso.
- **R50**. Antes de la entrega, revisar riesgo de inyección SQL, XSS, CSRF, JWT mal verificados, secretos expuestos y divulgación de datos.

## 8. Calidad adicional recomendada
- **R51**. Agregar pruebas unitarias a servicios/reglas y pruebas de integración a rutas, autenticación y repositorios.
- **R52**. Probar accesos no autorizados, entradas inválidas, caducidad JWT, errores de base de datos y flujos importantes.
- **R53**. Automatizar formato, lint, revisión de tipos y pruebas en CI cuando exista infraestructura.
- **R54**. Mantener un `.env.example` sin secretos y excluir `.env` y credenciales del versionamiento.
- **R55**. Mantener README con instalación, variables, migraciones, ejecución, pruebas y endpoints.
- **R56**. Usar commits descriptivos; hacer revisión de código y comprobar que no se incluyen secretos ni cambios ajenos.

## Reglas específicas para una IA de programación
1. Antes de editar, inspecciona la estructura, identifica el módulo existente y explica brevemente qué archivos tocarás.
2. Respeta arquitectura por módulos y las capas Route → Controller → Service → Repository.
3. Nunca pongas reglas de negocio en controllers, SQL en controllers o lógica HTTP en services.
4. Reutiliza convenciones, utilidades, componentes, middlewares y herramientas existentes; no agregues dependencias sin razón documentada.
5. Valida entradas y permisos del lado del servidor; usa respuestas HTTP correctas.
6. Nunca concatenes SQL con entrada de usuario ni renderices HTML no confiable directamente.
7. Nunca reveles secretos, hashes ni modelos completos en respuestas, tokens o logs.
8. Implementa estados visibles de carga, éxito y error y prevé dobles envíos.
9. Incluye o ajusta pruebas relevantes de los cambios; no inventes resultados de ejecución.
10. Entrega un resumen de archivos modificados, decisiones, verificaciones ejecutadas y pendientes conocidos.

## Checklist para cada entrega
- [ ] Código organizado por módulo y capa
- [ ] Contratos API, HTTP y errores correctos
- [ ] Entradas y permisos validados en servidor
- [ ] Consultas parametrizadas y restricciones de datos
- [ ] Respuestas sin datos sensibles
- [ ] JWT y secretos correctamente protegidos (si aplica)
- [ ] Interfaz con feedback, consistencia y errores comprensibles
- [ ] Pruebas relevantes ejecutadas y verificadas
- [ ] Sin credenciales en el repositorio
- [ ] Cambios documentados y limitados al alcance

## Referencias de las clases
- Clase 3: qué es una API, HTTP, métodos y códigos de estado.
- Clase 4: elección de base de datos, SQL/NoSQL, relaciones y consistencia.
- Clase 5: usabilidad, heurísticas de Nielsen y prevención/comunicación de errores.
- Clase 8: arquitectura backend en cuatro capas y organización por módulos.
- Clase 9: JWT, firma, payload, expiración, access y refresh token, almacenamiento.
- Clase 10: SQL Injection, XSS, CSRF, exposición de datos y errores de JWT.
