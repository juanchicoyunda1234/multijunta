# Implementation Plan: Base del sistema, acceso por roles y gestión de sesión

**Branch**: `001-base-acceso-sesion` | **Date**: 2026-11-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-base-acceso-sesion/spec.md`

## Summary

Crear el esqueleto de la aplicación Multi-Junta y su base de seguridad: inicio y cierre de
sesión con bloqueo por intentos (CU20), recuperación de contraseña atendida por la directiva
(CU21), cambio de contraseña obligatorio u opcional (CU22), campana de notificaciones (CU29
base), menú y permisos por rol según `docs/contexto/seguridad.md`, aislamiento por junta y
datos de prueba. Enfoque: aplicación Spring Boot 3 monolítica con vistas Thymeleaf
renderizadas en el servidor, Spring Security con formulario de inicio de sesión y BCrypt, una
matriz de permisos única en código que alimenta a la vez las reglas de acceso y el menú, y
repositorios que siempre filtran por la junta del usuario autenticado.

## Technical Context

**Language/Version**: Java 17 (código compatible con 17; el equipo de desarrollo tiene JDK 21)

**Primary Dependencies**: Spring Boot 3.5.x (spring-boot-starter-web, -data-jpa, -security,
-validation, -thymeleaf), Bootstrap 5.3 por CDN, H2

**Storage**: H2 en modo archivo `jdbc:h2:file:./data/multijunta`; consola H2 solo en perfil de
desarrollo; esquema generado por JPA (`ddl-auto=update`)

**Testing**: JUnit 5, Spring Boot Test, MockMvc, spring-security-test; H2 en memoria para
pruebas

**Target Platform**: servidor único (Windows o Linux) con JRE 17+; clientes en navegador de
computador y celular

**Project Type**: aplicación web monolítica con vistas en el servidor

**Performance Goals**: cada página responde en menos de 1 s con los volúmenes de una junta
(cientos de socios); inicio de sesión completo en menos de 30 s para el usuario (SC-001)

**Constraints**: sin comentarios en el código; textos en español; responsive desde 360 px;
sin envío de correos; sin frontend separado; Maven Wrapper incluido porque Maven no está
instalado en la máquina de desarrollo

**Scale/Scope**: decenas de juntas, hasta unos miles de usuarios en total; esta funcionalidad
abarca unas 12 pantallas (login, recuperación, cambio de contraseña, inicios provisionales,
solicitudes, notificaciones, en construcción, 403, 404)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento en este plan | Estado |
|---|---|---|
| I. Especificación como fuente de verdad | Todo componente sale de un FR de `spec.md`; sin generadores externos | ✅ |
| II. Aislamiento por junta | La junta se toma del usuario autenticado (`UsuarioActual`); repositorios con métodos `...AndJuntaId`; registro ajeno → 404; prueba A→B obligatoria | ✅ |
| III. Permisos por rol | Matriz única `MatrizPermisos` aplicada en `SecurityFilterChain` (GET para "ver", todo método para "✔"); página 403 en español; BCrypt | ✅ |
| IV. Español y responsive | Plantillas en español con Bootstrap 5; mensajes de validación en `messages.properties` en español | ✅ |
| V. Simplicidad | Un solo módulo Maven, Thymeleaf, H2; sin dialecto de layout ni librerías extra; fragmentos Thymeleaf simples | ✅ |
| VI. Código sin comentarios | Regla de revisión en tareas; ningún archivo fuente con comentarios | ✅ |
| VII. Verificación contra criterios | Cada escenario de aceptación tiene prueba MockMvc o paso en `quickstart.md` | ✅ |
| VIII. Trazabilidad CUxx | Contratos y pruebas indican el CU; `tasks.md` heredará la etiqueta | ✅ |

**Re-evaluación tras el diseño (Fase 1)**: sin violaciones. Principio II: SolicitudRecuperacion
guarda su `junta`; Notificacion se aísla por destinatario (su junta es la del usuario), lo que
se considera asociación suficiente según `data-model.md`. El único punto a vigilar es el uso
de `LocalDateTime` para instantes (bloqueo, solicitudes, notificaciones), justificado en
`research.md` (R7) como combinación de fecha y hora, compatible con la regla de tipos de
`tech.md`.

## Project Structure

### Documentation (this feature)

```text
specs/001-base-acceso-sesion/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── rutas-ui.md
│   └── matriz-permisos.md
└── tasks.md
```

### Source Code (repository root)

```text
pom.xml
mvnw, mvnw.cmd, .mvn/wrapper/
README.md
src/main/java/ec/uta/multijunta/
├── MultijuntaApplication.java
├── model/
│   ├── Junta.java, TipoJunta.java
│   ├── Usuario.java, Rol.java
│   ├── Socio.java, EstadoSocio.java
│   ├── Sector.java
│   ├── SolicitudRecuperacion.java, EstadoSolicitud.java
│   └── Notificacion.java
├── repository/
│   ├── JuntaRepository.java
│   ├── UsuarioRepository.java
│   ├── SocioRepository.java
│   ├── SectorRepository.java
│   ├── SolicitudRecuperacionRepository.java
│   └── NotificacionRepository.java
├── service/
│   ├── UsuarioActual.java
│   ├── AutenticacionService.java
│   ├── PasswordService.java
│   ├── RecuperacionService.java
│   ├── NotificacionService.java
│   ├── MenuService.java
│   └── ReglaNegocioException.java
├── rest/
│   ├── LoginController.java
│   ├── InicioController.java
│   ├── PasswordController.java
│   ├── RecuperacionController.java
│   ├── NotificacionController.java
│   ├── EnConstruccionController.java
│   └── VistaComunAdvice.java
└── config/
    ├── SeguridadConfig.java
    ├── MatrizPermisos.java
    ├── UsuarioDetailsService.java
    ├── UsuarioAutenticado.java
    ├── ManejadorFalloLogin.java
    ├── EventosAutenticacion.java
    ├── EstadoCuentaFilter.java
    ├── CambioPasswordFilter.java
    ├── ManejadorAccesoDenegado.java
    └── DatosPrueba.java
src/main/resources/
├── application.properties
├── application-dev.properties
├── messages.properties
├── static/css/app.css
└── templates/
    ├── fragments/layout.html
    ├── auth/login.html, auth/recuperar.html
    ├── cuenta/cambiar-password.html
    ├── inicio/plataforma.html, inicio/directiva.html, inicio/socio.html
    ├── recuperacion/solicitudes.html
    ├── notificaciones/lista.html
    ├── en-construccion.html
    └── error/403.html, error/404.html, error/500.html
src/test/java/ec/uta/multijunta/
├── LoginTest.java
├── CambioPasswordTest.java
├── RecuperacionTest.java
├── NotificacionTest.java
├── PermisosPorRolTest.java
├── AislamientoJuntaTest.java
└── DatosPruebaTest.java
src/test/resources/application-test.properties
```

**Structure Decision**: proyecto único Maven con las capas de `docs/contexto/structure.md`
(`rest → service → repository → model`, `config` transversal). Los filtros y la configuración
de Spring Security viven en `config`; las reglas de negocio (bloqueo, contraseñas,
recuperación, notificaciones, aislamiento) en `service`. Las pruebas se agrupan por caso de
uso.

## Complexity Tracking

Sin violaciones a la constitución; no se requiere justificación.
