---

description: "Lista de tareas de la funcionalidad 001-base-acceso-sesion"
---

# Tasks: Base del sistema, acceso por roles y gestión de sesión

**Input**: Design documents from `/specs/001-base-acceso-sesion/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: incluidas. La constitución (Principio VII) exige verificar cada caso de uso contra
sus criterios de aceptación, y `plan.md` define una clase de prueba por caso de uso. Se
escriben antes de la implementación de cada historia y deben fallar primero.

**Organization**: tareas agrupadas por historia de usuario de `spec.md`. Cada tarea de
historia indica el CU que cubre (Principio VIII).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia de `spec.md` (US1 a US6)

## Path Conventions

- Código: `src/main/java/ec/uta/multijunta/` (abreviado `JAVA/` en esta lista)
- Recursos: `src/main/resources/` (abreviado `RES/`)
- Pruebas: `src/test/java/ec/uta/multijunta/` (abreviado `TEST/`)
- Ningún archivo fuente lleva comentarios (Principio VI); todo texto visible en español
  (Principio IV).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: crear el proyecto Maven ejecutable

- [X] T001 Crear `pom.xml` en la raíz: groupId `ec.uta`, artifactId `multijunta`, Java 17, padre Spring Boot 3.5.x, dependencias starter web, data-jpa, security, validation, thymeleaf, h2 (runtime), starter-test y spring-security-test (test)
- [X] T002 Agregar Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) en la raíz según research.md R11
- [X] T003 [P] Crear `JAVA/MultijuntaApplication.java` con `@SpringBootApplication`
- [X] T004 [P] Crear `RES/application.properties`: datasource `jdbc:h2:file:./data/multijunta`, `ddl-auto=update`, `server.servlet.session.timeout=30m`, `spring.messages.basename=messages`, `spring.thymeleaf.cache=false`, `server.error.whitelabel.enabled=false`
- [X] T005 [P] Crear `RES/application-dev.properties` con consola H2 habilitada en `/h2-console` y `src/test/resources/application-test.properties` con H2 en memoria
- [X] T006 [P] Crear `.gitignore` en la raíz con `target/`, `data/`, `uploads/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: entidades, repositorios, seguridad mínima y layout que todas las historias usan

**⚠️ CRITICAL**: ninguna historia empieza antes de terminar esta fase

- [X] T007 [P] Crear enums `JAVA/model/Rol.java`, `JAVA/model/TipoJunta.java`, `JAVA/model/EstadoSocio.java` con los valores de data-model.md (Enums)
- [X] T008 [P] Crear entidad `JAVA/model/Junta.java` con validaciones de data-model.md Junta.nombre, Junta.tipo, Junta.ruc, Junta.direccion, Junta.telefono, Junta.provincia, Junta.canton, Junta.fechaRegistro, Junta.logoUrl, Junta.montoMultaInasistencia, Junta.activa, Junta.motivoSuspension
- [X] T009 [P] Crear entidad `JAVA/model/Usuario.java` con herencia `JOINED` (research.md R1) y campos de data-model.md Usuario.nombres, Usuario.apellidos, Usuario.cedula, Usuario.email, Usuario.username, Usuario.password, Usuario.rol, Usuario.junta, Usuario.activo, Usuario.debeCambiarPassword, Usuario.intentosFallidos, Usuario.bloqueadoHasta; normalizar `username` a minúsculas y sin espacios en `@PrePersist`/`@PreUpdate`
- [X] T010 [P] Crear entidad `JAVA/model/Sector.java` con data-model.md Sector.nombre, Sector.descripcion, Sector.junta (restricción única nombre + junta)
- [X] T011 Crear entidad `JAVA/model/Socio.java` que extiende `Usuario` con data-model.md Socio.codigoSocio, Socio.fechaIngreso, Socio.estado, Socio.celular, Socio.tieneCuenta, Socio.sector (depende de T009, T010)
- [X] T012 [P] Crear `JAVA/repository/JuntaRepository.java`, `JAVA/repository/UsuarioRepository.java` (`findByUsername`, `findByCedula`, `findByIdAndJuntaId`; unicidad global de data-model.md Usuario.cedula y Usuario.username), `JAVA/repository/SocioRepository.java` y `JAVA/repository/SectorRepository.java` (métodos solo con `juntaId`, research.md R5)
- [X] T013 Crear `JAVA/config/UsuarioAutenticado.java` (implementa `UserDetails`, guarda id, nombre completo, rol, juntaId y nombre de junta) y `JAVA/config/UsuarioDetailsService.java` que carga por `username` normalizado (depende de T012)
- [X] T014 Crear `JAVA/service/UsuarioActual.java` que expone usuario, rol y `juntaId` del contexto de seguridad, nunca de parámetros del navegador (FR-015)
- [X] T015 Crear `JAVA/config/SeguridadConfig.java` mínimo: `BCryptPasswordEncoder`, `formLogin` con página `/login`, rutas públicas `/login`, `/recuperar`, `/css/**` y `/error`, resto autenticado (FR-001, FR-021); con el perfil `dev`, abrir `/h2-console/**` sin CSRF y con `frameOptions sameOrigin` (research.md R4)
- [X] T016 [P] Crear `RES/messages.properties` con todas las claves y textos de contracts/rutas-ui.md (Mensajes exactos) y mensajes de validación en español
- [X] T017 [P] Crear `RES/static/css/app.css` con paleta blanco/negro y verde #16A34A para botones primarios y estilos responsive desde 360 px (research.md R10)
- [X] T018 Crear `RES/templates/fragments/layout.html` con cabecera (Bootstrap 5.3 por CDN), barra superior con espacio para nombre, rol, junta, campana, "Cambiar contraseña" y "Cerrar Sesión" (POST con CSRF), menú lateral offcanvas en celular y bloque de mensajes flash (FR-019, FR-039)
- [X] T019 [P] Crear `RES/templates/error/403.html`, `RES/templates/error/404.html` y `RES/templates/error/500.html` en español con enlace a `/inicio` (FR-014, FR-020)
- [X] T020 Crear `JAVA/service/ReglaNegocioException.java` (excepción con clave de mensaje) y `JAVA/service/RecursoNoEncontradoException.java` que se responde como 404 (FR-016)

**Checkpoint**: la aplicación arranca, muestra `/login` y protege el resto de rutas

---

## Phase 3: User Story 1 - Iniciar y cerrar sesión según el rol (Priority: P1) 🎯 MVP — CU20

**Goal**: cualquier usuario entra con usuario y contraseña, llega a su inicio según rol y
cierra sesión; se aplican bloqueo, cuenta desactivada y junta suspendida.

**Independent Test**: `LoginTest` en verde y pasos 1 a 7 de quickstart.md.

### Tests for User Story 1 ⚠️

- [X] T021 [P] [US1] Escribir `TEST/LoginTest.java` (CU20) con un caso por escenario de aceptación HU1-1 a HU1-11 de spec.md y por los casos borde de espacios y mayúsculas en el usuario, sesión expirada, y cuenta desactivada o junta suspendida durante la sesión

### Implementation for User Story 1

- [X] T022 [P] [US1] Crear `JAVA/config/JuntaSuspendidaException.java` (subclase de `AccountStatusException`) para CU20
- [X] T023 [US1] Implementar `JAVA/service/AutenticacionService.java` (CU20): `registrarFallo(username)` y `registrarExito(username)` según las transiciones de data-model.md Usuario.intentosFallidos y Usuario.bloqueadoHasta (5 fallos → 15 min; un usuario inexistente no cuenta); `estaBloqueado(usuario)`
- [X] T024 [US1] Crear `JAVA/config/EventosAutenticacion.java` (CU20) que escucha `AuthenticationFailureBadCredentialsEvent` y `AuthenticationSuccessEvent` y delega en `AutenticacionService` (research.md R3)
- [X] T025 [US1] Crear `JAVA/config/ManejadorFalloLogin.java` (CU20) que redirige a `/login?error`, `?bloqueada`, `?desactivada` o `?suspendida` según la excepción; ante `BadCredentialsException` vuelve a consultar la cuenta y, si quedó bloqueada con ese intento, redirige a `?bloqueada` (FR-005 a FR-007, research.md R3)
- [X] T026 [US1] Ampliar `JAVA/config/SeguridadConfig.java` (CU20): `DaoAuthenticationProvider` con `preAuthenticationChecks` solo de bloqueo y `postAuthenticationChecks` de data-model.md Usuario.activo y Junta.activa (research.md R2), manejador de fallo, éxito a `/inicio`, logout POST a `/login?salio`, `invalidSessionUrl("/login?expirada")`
- [X] T027 [US1] Crear `JAVA/config/EstadoCuentaFilter.java` (CU20 FR-010) que recarga el usuario en cada petición e invalida la sesión si la cuenta está desactivada o la junta suspendida, y registrarlo en `SeguridadConfig`
- [X] T028 [US1] Crear `JAVA/rest/LoginController.java` (CU20) para GET `/login` y `RES/templates/auth/login.html` con usuario, contraseña, botón "Iniciar Sesión", enlace "¿Olvidaste tu contraseña?" y el mensaje según parámetro (contracts/rutas-ui.md)
- [X] T029 [US1] Crear `JAVA/rest/InicioController.java` (CU20 FR-003): `/inicio` redirige por rol a `/plataforma`, `/panel` o `/mi-informacion`, más las tres pantallas provisionales `RES/templates/inicio/plataforma.html`, `RES/templates/inicio/directiva.html`, `RES/templates/inicio/socio.html` que usan el layout
- [X] T030 [US1] Ejecutar `TEST/LoginTest.java` y corregir hasta que pase completo

**Checkpoint**: inicio y cierre de sesión funcionales para los cinco roles

---

## Phase 4: User Story 2 - Acceso por rol y aislamiento por junta (Priority: P1)

**Goal**: el menú y los permisos siguen la matriz de seguridad; nadie ve datos de otra junta.

**Independent Test**: `PermisosPorRolTest` y `AislamientoJuntaTest` en verde; pasos 8 a 10 de
quickstart.md.

### Tests for User Story 2 ⚠️

- [X] T031 [P] [US2] Escribir `TEST/PermisosPorRolTest.java` parametrizada sobre cada fila × rol de contracts/matriz-permisos.md (C, L, —) más los casos HU2-2, HU2-3, HU2-5 y HU2-6 de spec.md
- [X] T032 [P] [US2] Escribir `TEST/AislamientoJuntaTest.java` (HU2-4, Principio II): a nivel de repositorio, `findByIdAndJuntaId` de usuario y sector con el id de un registro de la junta B y el `juntaId` de la junta A devuelve vacío; a nivel de servicio, `UsuarioActual` ignora cualquier `juntaId` enviado en parámetros. Las pruebas por URL entre juntas (restablecer solicitud ajena, abrir notificación ajena → 404 sin datos) se agregan en T050 y T057

### Implementation for User Story 2

- [X] T033 [US2] Crear `JAVA/config/MatrizPermisos.java` con exactamente las filas de contracts/matriz-permisos.md (CU, texto de menú, ícono, prefijo, roles C, roles L)
- [X] T034 [US2] Ampliar `JAVA/config/SeguridadConfig.java` para registrar desde `MatrizPermisos` los `requestMatchers`: GET para roles C + L, demás métodos solo roles C, prefijos más específicos primero (`/asistencia/correcciones` antes de `/asistencia`), `/plataforma` como ruta exacta, y negar el resto salvo las rutas públicas de T015 (FR-013, FR-017, contracts/matriz-permisos.md)
- [X] T035 [P] [US2] Crear `JAVA/config/ManejadorAccesoDenegado.java` que responde la vista `error/403` con estado 403 y registrarlo en `SeguridadConfig` (FR-014)
- [X] T036 [US2] Crear `JAVA/service/MenuService.java` que devuelve las opciones de `MatrizPermisos` con C o L para el rol actual, en el orden de la matriz (FR-018)
- [X] T037 [US2] Crear `JAVA/rest/VistaComunAdvice.java` (`@ControllerAdvice`) que agrega al modelo el menú, nombre, rol y nombre de junta, y traduce `RecursoNoEncontradoException` a la vista `error/404` con estado 404 (FR-016, FR-019)
- [X] T038 [US2] Completar `RES/templates/fragments/layout.html` para pintar el menú lateral y los datos de la barra superior desde el modelo (no mostrar junta al SUPER_ADMIN)
- [X] T039 [US2] Crear `JAVA/rest/EnConstruccionController.java` y `RES/templates/en-construccion.html`: los prefijos de `MatrizPermisos` sin módulo construido redirigen a `/en-construccion` (FR-018)
- [X] T040 [US2] Ejecutar `TEST/PermisosPorRolTest.java` y `TEST/AislamientoJuntaTest.java` y corregir hasta que pasen

**Checkpoint**: menú por rol, 403 y 404 en español, aislamiento comprobado

---

## Phase 5: User Story 3 - Cambiar contraseña (Priority: P1) — CU22

**Goal**: todo usuario cambia su contraseña; las cuentas marcadas quedan obligadas a hacerlo.

**Independent Test**: `CambioPasswordTest` en verde; paso 3 y 14 de quickstart.md.

### Tests for User Story 3 ⚠️

- [X] T041 [P] [US3] Escribir `TEST/CambioPasswordTest.java` (CU22) con los escenarios HU3-1 a HU3-7 y HU1-5 de spec.md, más el caso de cerrar sesión estando obligado

### Implementation for User Story 3

- [X] T042 [P] [US3] Crear `JAVA/service/PasswordService.java` (CU22): `validarFormato` (FR-031), `cambiar(usuario, actual, nueva, confirmacion)` con las tres excepciones de FR-029 lanzando `ReglaNegocioException`, actualización de data-model.md Usuario.password con BCrypt y Usuario.debeCambiarPassword a `false`; `generarTemporal()` según research.md R8
- [X] T043 [US3] Crear `JAVA/rest/PasswordController.java` (CU22) para GET/POST `/cuenta/cambiar-password` y `RES/templates/cuenta/cambiar-password.html`; éxito redirige a `/inicio` con "Contraseña actualizada correctamente"
- [X] T044 [US3] Crear `JAVA/config/CambioPasswordFilter.java` (CU20 alterno, FR-004) que redirige a `/cuenta/cambiar-password` mientras data-model.md Usuario.debeCambiarPassword sea verdadero, permitiendo esa ruta, `/logout` y estáticos; registrarlo en `SeguridadConfig` después de `EstadoCuentaFilter`
- [X] T045 [US3] Ejecutar `TEST/CambioPasswordTest.java` y corregir hasta que pase

**Checkpoint**: cambio de contraseña libre y obligatorio funcionando

---

## Phase 6: User Story 6 - Datos de prueba al arrancar (Priority: P1)

**Goal**: al arrancar con la base vacía existen juntas, usuarios por rol, sectores y socios.

**Independent Test**: `DatosPruebaTest` en verde; pasos 2 y 16 de quickstart.md.

### Tests for User Story 6 ⚠️

- [X] T046 [P] [US6] Escribir `TEST/DatosPruebaTest.java` (HU6-1, HU6-2, HU6-4) que ejecuta el cargador dos veces y comprueba conteos, roles por junta, junta suspendida, usuario desactivado, usuario obligado a cambiar contraseña y ausencia de duplicados

### Implementation for User Story 6

- [X] T047 [US6] Crear `JAVA/config/DatosPrueba.java` (`CommandLineRunner`, `@Profile("!test")` y también invocable desde la prueba) según research.md R12, que solo carga si no hay juntas; RUC de 13 dígitos (data-model.md Junta.ruc), usernames normalizados y únicos (data-model.md Usuario.username), cédulas distintas en todas las juntas (data-model.md Usuario.cedula), socios con sector de su junta (data-model.md Socio.sector) y `tieneCuenta`/`activo` coherentes (data-model.md Socio.tieneCuenta)
- [X] T048 [US6] Crear `README.md` en la raíz con requisitos, cómo ejecutar (`.\mvnw.cmd spring-boot:run`, `.\mvnw.cmd test`) y tabla de usuarios de prueba con usuario, contraseña, rol y junta (FR-038)
- [X] T049 [US6] Ejecutar `TEST/DatosPruebaTest.java` y corregir hasta que pase

**Checkpoint**: MVP completo (US1, US2, US3, US6) verificable con los usuarios del README

---

## Phase 7: User Story 4 - Recuperar contraseña con apoyo de la directiva (Priority: P2) — CU21

**Goal**: el usuario solicita recuperación; el administrador correcto la atiende y entrega una
contraseña temporal.

**Independent Test**: `RecuperacionTest` en verde; pasos 11, 13 y 14 de quickstart.md.

### Tests for User Story 4 ⚠️

- [X] T050 [P] [US4] Escribir `TEST/RecuperacionTest.java` (CU21) con los escenarios HU4-1 a HU4-8 de spec.md, más: solicitud del SUPER_ADMIN no crea registro, cuenta bloqueada queda desbloqueada al restablecer, ADMIN_JUNTA de la junta B recibe 404 al restablecer una solicitud de la junta A

### Implementation for User Story 4

- [X] T051 [P] [US4] Crear `JAVA/model/EstadoSolicitud.java` y entidad `JAVA/model/SolicitudRecuperacion.java` con data-model.md SolicitudRecuperacion.usuario, SolicitudRecuperacion.junta, SolicitudRecuperacion.fechaSolicitud, SolicitudRecuperacion.estado, SolicitudRecuperacion.fechaAtencion, SolicitudRecuperacion.atendidaPor
- [X] T052 [US4] Crear `JAVA/repository/SolicitudRecuperacionRepository.java` con consultas de pendientes por junta excluyendo ADMIN_JUNTA, pendientes de ADMIN_JUNTA (para SUPER_ADMIN) y `existsByUsuarioIdAndEstado`
- [X] T053 [US4] Implementar `JAVA/service/RecuperacionService.java` (CU21): `solicitar(identificador)` según FR-023 (busca primero por data-model.md Usuario.username y luego por Usuario.cedula; copia la junta del solicitante en SolicitudRecuperacion.junta; sin duplicar pendiente, sin SUPER_ADMIN, sin cuenta inexistente o inactiva, siempre sin revelar), `listarAtendibles()` según FR-024 usando `UsuarioActual`, `restablecer(id)` según FR-025 y FR-026 (usa `PasswordService.generarTemporal`, reinicia data-model.md Usuario.intentosFallidos y Usuario.bloqueadoHasta, pone Usuario.debeCambiarPassword en `true`; solicitud ajena → `RecursoNoEncontradoException`; ya atendida → `ReglaNegocioException`)
- [X] T054 [US4] Crear `JAVA/rest/RecuperacionController.java` (CU21) para GET/POST `/recuperar`, GET `/recuperacion/solicitudes` y POST `/recuperacion/solicitudes/{id}/restablecer` con la contraseña temporal como atributo flash (research.md R8)
- [X] T055 [P] [US4] Crear `RES/templates/auth/recuperar.html` y `RES/templates/recuperacion/solicitudes.html` (nombre, cédula, rol, fecha, botón "Restablecer" con confirmación y recuadro de contraseña temporal mostrada una vez)
- [X] T056 [US4] Ejecutar `TEST/RecuperacionTest.java` y corregir hasta que pase

**Checkpoint**: recuperación de contraseña de punta a punta

---

## Phase 8: User Story 5 - Campana de notificaciones (Priority: P2) — CU29 base

**Goal**: campana con contador de no leídas, lista, abrir y marcar todas; avisos de
solicitudes de recuperación.

**Independent Test**: `NotificacionTest` en verde; paso 12 de quickstart.md.

**Dependency**: T061 integra con `RecuperacionService` (Phase 7).

### Tests for User Story 5 ⚠️

- [X] T057 [P] [US5] Escribir `TEST/NotificacionTest.java` (CU29) con los escenarios HU5-1 a HU5-6 de spec.md, incluido que abrir la notificación de otro usuario responde 404

### Implementation for User Story 5

- [X] T058 [P] [US5] Crear entidad `JAVA/model/Notificacion.java` con data-model.md Notificacion.usuario, Notificacion.titulo, Notificacion.mensaje, Notificacion.fecha, Notificacion.leida, Notificacion.enlace y `JAVA/repository/NotificacionRepository.java` (`countByUsuarioIdAndLeidaFalse`, lista ordenada no leídas primero y fecha descendente, `findByIdAndUsuarioId`)
- [X] T059 [US5] Implementar `JAVA/service/NotificacionService.java` (CU29): `crear(usuario, titulo, mensaje, enlace)`, `contarNoLeidas()`, `listar()`, `abrir(id)` que marca leída y devuelve el enlace (ajena → `RecursoNoEncontradoException`), `marcarTodas()` (FR-032 a FR-036)
- [X] T060 [US5] Crear `JAVA/rest/NotificacionController.java` (CU29) para GET `/notificaciones`, GET `/notificaciones/{id}` y POST `/notificaciones/marcar-todas`, y `RES/templates/notificaciones/lista.html` con no leídas destacadas
- [X] T061 [US5] Integrar en `JAVA/service/RecuperacionService.java` (CU21/CU29 FR-027): al registrar una solicitud, notificar a cada ADMIN_JUNTA activo de la junta o al SUPER_ADMIN si quien solicita es ADMIN_JUNTA, con enlace `/recuperacion/solicitudes`
- [X] T062 [US5] Agregar el contador a `JAVA/rest/VistaComunAdvice.java` y la campana con insignia a `RES/templates/fragments/layout.html` (sin número cuando es 0)
- [X] T063 [US5] Ejecutar `TEST/NotificacionTest.java` y corregir hasta que pase

**Checkpoint**: todas las historias funcionan de forma independiente

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: puertas de calidad de la constitución

- [X] T064 Ejecutar `.\mvnw.cmd test` completo y dejar todas las pruebas en verde
- [X] T065 [P] Revisar que ningún archivo bajo `src/` tenga comentarios (`//`, `/*`, `<!--`, `#` en properties) y eliminarlos (Principio VI)
- [X] T066 [P] Revisar que todos los textos visibles en `RES/templates/` y `RES/messages.properties` estén en español y que las pantallas no tengan desplazamiento horizontal a 360 px (SC-006)
- [X] T067 Ejecutar la validación manual de `specs/001-base-acceso-sesion/quickstart.md` (pasos 1 a 16) con `.\mvnw.cmd spring-boot:run` y anotar resultados
- [X] T068 Verificar trazabilidad: cada FR de spec.md cubierto por al menos una tarea y una prueba con su CUxx (Principio VIII)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias
- **Foundational (Phase 2)**: depende de Setup; bloquea todas las historias
- **US1 (Phase 3)**: depende de Foundational
- **US2 (Phase 4)**: depende de Foundational; comparte `SeguridadConfig` con US1, así que se hace después de US1
- **US3 (Phase 5)**: depende de Foundational; `CambioPasswordFilter` se registra tras `EstadoCuentaFilter` (US1)
- **US6 (Phase 6)**: depende solo de Foundational
- **US4 (Phase 7)**: depende de US3 (`PasswordService.generarTemporal`)
- **US5 (Phase 8)**: independiente salvo T061, que depende de US4
- **Polish (Phase 9)**: depende de todas las historias

### Within Each User Story

- Pruebas primero y deben fallar
- Modelos → servicios → controladores y vistas → ejecución de pruebas

### Parallel Opportunities

- Setup: T003, T004, T005, T006
- Foundational: T007, T008, T009, T010, T016, T017, T019 en paralelo; luego T011, T012
- US6 puede hacerse en paralelo con US1 a US3 tras Foundational
- Dentro de cada historia, la prueba [P] en paralelo con el primer modelo o servicio [P]

---

## Parallel Example: User Story 4

```text
Task: "Escribir TEST/RecuperacionTest.java (CU21)"
Task: "Crear JAVA/model/SolicitudRecuperacion.java"
Task: "Crear RES/templates/auth/recuperar.html y recuperacion/solicitudes.html"
```

---

## Implementation Strategy

### MVP First

1. Phase 1 Setup y Phase 2 Foundational
2. Phase 3 (US1, CU20) → validar pasos 1 a 7 de quickstart.md
3. Phases 4, 5 y 6 (US2, US3, US6) → MVP con seguridad completa y datos de prueba

### Incremental Delivery

1. MVP (US1 + US2 + US3 + US6)
2. US4 (CU21) → validar recuperación
3. US5 (CU29 base) → validar campana
4. Polish → puertas de calidad de la constitución

---

## Notes

- [P] = archivos distintos, sin dependencias pendientes
- La etiqueta [USx] y el CUxx en la descripción dan la trazabilidad exigida por la constitución
- Detenerse en cada checkpoint para validar la historia

---

## Phase 10: Convergence

- [X] T069 Alinear `specs/001-base-acceso-sesion/contracts/matriz-permisos.md` con HU2-6 de spec.md: cambiar "Cualquier otra ruta no listada se niega con 403" por "requiere sesión iniciada y, si no existe, responde 404", que es lo que implementa `JAVA/config/SeguridadConfig.java` (`anyRequest().authenticated()`) per plan: contracts/matriz-permisos.md (contradicts)
- [X] T070 Hacer cumplir la unicidad de data-model.md Socio.codigoSocio dentro de la junta: agregar `existsByCodigoSocioAndJuntaId` en `JAVA/repository/SocioRepository.java` y una validación en la capa `service` usada antes de guardar socios, con prueba en `TEST/AislamientoJuntaTest.java` o una nueva `TEST/SocioTest.java` per data-model: Socio.codigoSocio (partial)
- [X] T071 Validar en `JAVA/model/Usuario.java` que data-model.md Usuario.junta sea obligatorio para todo rol distinto de SUPER_ADMIN (por ejemplo con un método `@AssertTrue` de Bean Validation) y agregar un caso que lo compruebe per FR-012 (partial)
- [X] T072 Mostrar nombre, rol y junta en la barra superior también en celular (hoy el bloque usa `d-none d-md-block` en `RES/templates/fragments/layout.html` y solo se ve dentro del menú desplegable), sin provocar desplazamiento horizontal a 360 px per FR-019 (partial)
- [X] T073 Usar la clave `notificacion.recuperacion.titulo` de `RES/messages.properties` en `JAVA/service/RecuperacionService.java` (vía `MessageSource`) en lugar del texto fijo per contracts/rutas-ui.md: Mensajes exactos (partial)
- [X] T074 Documentar en `specs/001-base-acceso-sesion/contracts/rutas-ui.md` la ruta GET `/` que `JAVA/rest/InicioController.java` atiende como alias de `/inicio`, o quitarla del controlador per plan: contracts/rutas-ui.md (unrequested)
