# Research: Base del sistema, acceso por roles y gestión de sesión

No quedaron puntos NEEDS CLARIFICATION en el contexto técnico. Estas son las decisiones de
diseño y su justificación.

## R1. Herencia Socio → Usuario

- **Decision**: herencia JPA `JOINED`: tabla `usuario` común y tabla `socio` con sus campos.
- **Rationale**: respeta la generalización del modelo de dominio; el inicio de sesión consulta
  una sola tabla para todos los roles; los socios sin cuenta también son filas de `usuario`
  con `activo = false` y `tieneCuenta = false`.
- **Alternatives considered**: `SINGLE_TABLE` (columnas nulas para la directiva); entidades
  separadas con relación 1 a 1 (rompe la generalización del diagrama).

## R2. Orden de las validaciones de inicio de sesión

- **Decision**: `DaoAuthenticationProvider` con `preAuthenticationChecks` que solo revisa el
  bloqueo temporal (`LockedException`) y `postAuthenticationChecks` que revisa cuenta
  desactivada (`DisabledException`) y junta suspendida (`JuntaSuspendidaException`, subclase
  de `AccountStatusException`). Un `ManejadorFalloLogin` traduce cada excepción al mensaje
  exacto de la spec.
- **Rationale**: la spec exige que "desactivada" y "suspendida" solo aparezcan con
  credenciales correctas (no revelar cuentas) y que el bloqueo se muestre aunque la
  contraseña sea correcta.
- **Alternatives considered**: validar todo en un controlador propio de login (duplica lo que
  Spring Security ya resuelve: sesión, CSRF, fijación de sesión).

## R3. Contador de intentos fallidos y bloqueo

- **Decision**: escuchar `AuthenticationFailureBadCredentialsEvent` y
  `AuthenticationSuccessEvent` en `EventosAutenticacion`, que delegan en
  `AutenticacionService.registrarFallo/registrarExito`. Al quinto fallo seguido:
  `bloqueadoHasta = ahora + 15 min` e `intentosFallidos = 0`. Un usuario inexistente no
  genera conteo. Como el evento se publica antes de invocar el manejador de fallo,
  `ManejadorFalloLogin` vuelve a consultar la cuenta ante `BadCredentialsException`: si quedó
  bloqueada, redirige a `/login?bloqueada`, de modo que el quinto intento ya muestra el
  mensaje de bloqueo.
- **Rationale**: lógica de negocio en `service`, enganchada con los eventos estándar.
- **Alternatives considered**: contar en el manejador de fallo (mezcla presentación y regla).

## R4. Matriz de permisos única

- **Decision**: `MatrizPermisos` declara cada caso de uso con su prefijo de ruta, su texto e
  ícono de menú, los roles con acceso completo y los roles de solo lectura. `SeguridadConfig`
  la recorre para registrar `requestMatchers`: `GET` para roles completos + lectura; otros
  métodos solo roles completos. `MenuService` usa la misma matriz para construir el menú.
- Prefijos más específicos se registran primero (`/asistencia/correcciones` antes de
  `/asistencia`); `/plataforma` se registra como ruta exacta. `/error` es pública y
  `/h2-console/**` solo se abre con el perfil `dev` (sin CSRF y con `frameOptions sameOrigin`).
- **Rationale**: una sola fuente evita que menú y permisos se desalineen; fiel a
  `docs/contexto/seguridad.md`. Los permisos especiales (CU28 autoriza/solicita, CU18 del
  socio, CU16 actas públicas) se refinan en la capa `service` de sus funcionalidades.
- **Alternatives considered**: `@PreAuthorize` en cada controlador (dispersa la matriz);
  tabla de permisos en base de datos (complejidad sin necesidad, la matriz es fija).

## R5. Aislamiento por junta

- **Decision**: `UsuarioActual` (servicio) expone el usuario y el `juntaId` de la sesión. Todo
  repositorio de entidades de junta ofrece solo métodos con `juntaId`
  (`findByIdAndJuntaId`, `findAllByJuntaId`). Un `Optional` vacío se convierte en 404.
- **Rationale**: explícito, fácil de revisar y de probar; sin magia de filtros de Hibernate.
- **Alternatives considered**: `@Filter` de Hibernate activado por petición (más difícil de
  verificar y fácil de olvidar en consultas nativas); esquemas separados por junta (excesivo).

## R6. Cambio de contraseña obligatorio y estado de la cuenta durante la sesión

- **Decision**: dos filtros después de la autenticación. `EstadoCuentaFilter` recarga el
  usuario en cada petición; si está desactivado o su junta suspendida, invalida la sesión y
  redirige a `/login?desactivada` o `/login?suspendida`. `CambioPasswordFilter` redirige a
  `/cuenta/cambiar-password` mientras `debeCambiarPassword` sea verdadero, permitiendo solo esa
  ruta, `/logout` y recursos estáticos.
- **Rationale**: cumple FR-004 y FR-010; la recarga por petición es barata con H2 local.
- **Alternatives considered**: `SessionRegistry` para expirar sesiones al desactivar (requiere
  que CU04/CU23/CU24 conozcan la seguridad).

## R7. Tipos de fecha y hora

- **Decision**: `LocalDate` para fechas de calendario (fechaRegistro, fechaIngreso);
  `LocalDateTime` para instantes (bloqueadoHasta, fechaSolicitud, fechaAtencion, fecha de
  notificación); `BigDecimal` para montos (montoMultaInasistencia).
- **Rationale**: un instante necesita fecha y hora juntas para calcular los 15 minutos y
  ordenar; es la combinación de `LocalDate` y `LocalTime` que pide `tech.md`.
- **Alternatives considered**: `Instant` (menos legible en vistas; no hay zonas horarias
  múltiples).

## R8. Contraseñas temporales y su muestra única

- **Decision**: `PasswordService.generarTemporal()` con `SecureRandom`: 10 caracteres de
  letras y dígitos sin ambiguos (0/O, 1/l/I), con al menos una letra y un dígito. Se muestra
  mediante atributo flash tras redirección, así que recargar la página no la vuelve a mostrar.
- **Rationale**: cumple FR-025 y la regla de muestra única sin guardar la contraseña en claro.
- **Alternatives considered**: mostrarla en la respuesta del POST (se repite al recargar).

## R9. Sesión, expiración y botón "atrás"

- **Decision**: `server.servlet.session.timeout=30m`; `invalidSessionUrl("/login?expirada")`;
  cabeceras `Cache-Control: no-store` por defecto de Spring Security; logout por POST con CSRF.
- **Rationale**: cumple FR-008 y FR-009 con configuración estándar.

## R10. Diseño de vistas

- **Decision**: fragmentos Thymeleaf (`fragments/layout.html`) con cabecera, barra superior
  (usuario, rol, junta, campana, menú de usuario) y menú lateral colapsable (offcanvas de
  Bootstrap en celular). Paleta blanco/negro con verde #16A34A en `app.css`. Contador de la
  campana y menú inyectados por `VistaComunAdvice` (`@ControllerAdvice`).
- **Rationale**: sin dependencias extra (no Layout Dialect), responsive con Bootstrap.

## R11. Build sin Maven instalado

- **Decision**: incluir Maven Wrapper (`mvnw`, `mvnw.cmd`); ejecución con
  `./mvnw spring-boot:run`.
- **Rationale**: la máquina de desarrollo no tiene Maven; el wrapper lo descarga una vez.

## R12. Datos de prueba

- **Decision**: `DatosPrueba` (`CommandLineRunner`) que solo inserta si no existe ninguna
  junta. Carga: SUPER_ADMIN; Junta 1 (RIEGO) y Junta 2 (COMUNITARIA) activas con ADMIN_JUNTA,
  SECRETARIO, TESORERO, 2 sectores y 3 socios (uno con cuenta, uno sin cuenta, uno INACTIVO);
  un usuario con `debeCambiarPassword`; uno desactivado; Junta 3 (PARROQUIAL) suspendida con
  un ADMIN_JUNTA. Contraseña común documentada en el README. Desactivado en el perfil `test`.
- **Rationale**: cubre todas las excepciones de CU20 y el aislamiento A/B (FR-037).

## R13. Estrategia de pruebas

- **Decision**: pruebas de integración con `@SpringBootTest` + MockMvc + `spring-security-test`
  sobre H2 en memoria, una clase por caso de uso, con datos propios por prueba.
  `PermisosPorRolTest` es parametrizada sobre la matriz completa (rol × CU) para SC-003.
- **Rationale**: verifica los criterios de aceptación de punta a punta (Principio VII).
