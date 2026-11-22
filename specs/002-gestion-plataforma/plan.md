# Implementation Plan: Módulo Gestión de Plataforma

**Branch**: `002-gestion-plataforma` | **Date**: 2026-11-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-gestion-plataforma/spec.md`

## Summary

Construir sobre la base de la funcionalidad 001 las pantallas de administración de la
plataforma y de la junta: listado, registro, edición y suspensión de juntas (CU01, CU24),
asignación de administradores (CU02), configuración de la junta con pestañas y sectores
(CU03), módulo Usuarios con pestañas Socios (CU04) y Directiva (CU23), y el panel de inicio de
la directiva con tarjetas por rol (CU25). Enfoque: nuevos servicios en la capa `service` por
caso de uso, controladores Thymeleaf en `rest`, ventanas emergentes y paneles laterales con
componentes de Bootstrap (sin frontend separado), un catálogo fijo de provincias y cantones
leído de un archivo de recursos, y reutilización directa de `MatrizPermisos`, `UsuarioActual`,
`PasswordService`, los filtros de sesión y el layout de 001.

## Technical Context

**Language/Version**: Java 17 (compila con el JDK 21 del equipo)

**Primary Dependencies**: Spring Boot 3.5.16 (web, data-jpa, security, validation, thymeleaf),
Bootstrap 5.3 e íconos por CDN, H2; sin dependencias nuevas

**Storage**: H2 en archivo `./data/multijunta` (sin cambios de esquema destructivos: solo se
usan columnas existentes); logos en `./uploads/logos`; catálogo de provincias y cantones en
`src/main/resources/catalogos/ecuador-dpa.csv`

**Testing**: JUnit 5, Spring Boot Test, MockMvc, spring-security-test, H2 en memoria (igual que
001)

**Target Platform**: servidor único con JRE 17+; navegador de computador y celular

**Project Type**: aplicación web monolítica con vistas en el servidor

**Performance Goals**: generar 50 credenciales en menos de 10 s (SC-003); páginas en menos de
1 s con volúmenes de una junta

**Constraints**: sin comentarios en el código; textos en español; responsive desde 360 px;
archivos subidos hasta 5 MB; sin correos

**Scale/Scope**: unas 8 pantallas nuevas o reemplazadas (juntas, panel de administrador,
configuración, usuarios con 2 pestañas, credenciales imprimibles, panel de la directiva) y 7
servicios

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento en este plan | Estado |
|---|---|---|
| I. Especificación como fuente de verdad | Cada servicio y vista nace de un FR de `spec.md`; los mockups CU01–CU04 guían las vistas | ✅ |
| II. Aislamiento por junta | Servicios de junta usan `UsuarioActual.juntaId()` y repositorios `...AndJuntaId`; la junta nunca viene del formulario; pruebas A/B por cada CU de junta | ✅ |
| III. Permisos por rol | Se reutiliza `MatrizPermisos` (filas `/plataforma/juntas`, `/junta/configuracion`, `/usuarios`, `/panel` ya existen); reglas finas de CU23 (solo SECRETARIO/TESORERO) en el servicio con 403 | ✅ |
| IV. Español y responsive | Vistas con Bootstrap, tablas en `.table-responsive`, modales y offcanvas usables en celular | ✅ |
| V. Simplicidad | Sin dependencias nuevas; catálogo en CSV de recursos; JavaScript mínimo en línea solo para cantones, pestañas y sectores | ✅ |
| VI. Código sin comentarios | Revisión en la fase de pulido | ✅ |
| VII. Verificación contra criterios | Una clase de prueba por CU con todos sus escenarios; quickstart manual | ✅ |
| VIII. Trazabilidad CUxx | Contratos, pruebas y tareas citan el CU | ✅ |

**Re-evaluación tras el diseño (Fase 1)**: sin violaciones. Se modifica código de 001 en tres
puntos acotados y justificados por la spec 002: inicio del SUPER_ADMIN (FR-001), menú del
SUPER_ADMIN sin la entrada duplicada "Inicio", y `EnConstruccionController` deja de cubrir las
rutas que esta funcionalidad construye.

## Project Structure

### Documentation (this feature)

```text
specs/002-gestion-plataforma/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── rutas-ui.md
│   └── mensajes.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/java/ec/uta/multijunta/
├── model/
│   ├── Provincia.java                 (record del catálogo, no entidad)
│   └── EstadoCuenta.java              (SIN_CUENTA, ACTIVA, DESACTIVADA)
├── repository/
│   ├── JuntaRepository.java           (+ existsByRuc, existsByRucAndIdNot)
│   ├── UsuarioRepository.java         (+ consultas de correo, conteo de administradores, directiva)
│   ├── SocioRepository.java           (+ countByJuntaIdAndEstado)
│   └── SectorRepository.java          (+ existsByJuntaIdAndNombreIgnoreCase)
├── service/
│   ├── CatalogoUbicacionService.java  (CU01, CU24)
│   ├── JuntaService.java              (CU01, CU24)
│   ├── AdministradorService.java      (CU02)
│   ├── ConfiguracionJuntaService.java (CU03)
│   ├── ArchivoService.java            (CU03 logo)
│   ├── CuentaSocioService.java        (CU04)
│   ├── DirectivaService.java          (CU23)
│   ├── PanelService.java              (CU25, incluye el record Tarjeta)
│   ├── Credencial.java                (record usuario + contraseña temporal)
│   ├── DatosJunta.java, DatosCuenta.java, DatosConfiguracion.java
│   │                                  (records que reciben los servicios; los formularios de rest los crean)
│   ├── CuentaFactory.java             (CU02, CU23: unicidad de cédula y correo, creación de cuenta)
│   └── ReglaNegocioException.java     (+ campo opcional)
├── rest/
│   ├── JuntaController.java           (CU01, CU02, CU24)
│   ├── ConfiguracionJuntaController.java (CU03)
│   ├── UsuariosController.java        (CU04, CU23)
│   ├── PanelController.java           (CU25)
│   ├── LogoController.java            (CU03)
│   ├── InicioController.java          (cambia: /plataforma redirige al listado)
│   ├── EnConstruccionController.java  (cambia: quita rutas construidas)
│   ├── VistaComunAdvice.java          (cambia: agrega logo de la junta)
│   └── formulario/
│       ├── JuntaFormulario.java
│       ├── CuentaFormulario.java          (base común de cédula, nombres, apellidos y correo)
│       ├── AdministradorFormulario.java   (extiende CuentaFormulario)
│       ├── ConfiguracionFormulario.java
│       └── DirectivaFormulario.java       (extiende CuentaFormulario)
└── config/
    ├── MatrizPermisos.java            (cambia: fila /plataforma sin texto de menú)
    └── EstadoCuentaFilter.java        (cambia: refresca el rol de la sesión, research R11)
src/main/resources/
├── application.properties             (+ multipart 5MB, carpeta de uploads)
├── messages.properties                (+ mensajes de contracts/mensajes.md)
├── catalogos/ecuador-dpa.csv
└── templates/
    ├── plataforma/juntas.html
    ├── junta/configuracion.html
    ├── usuarios/socios.html, usuarios/directiva.html, usuarios/credenciales.html
    ├── inicio/directiva.html          (reemplaza el provisional por las tarjetas)
    └── fragments/layout.html          (logo en la barra)
src/test/java/ec/uta/multijunta/
├── JuntaTest.java                     (CU01, CU24)
├── AdministradorTest.java             (CU02)
├── ConfiguracionJuntaTest.java        (CU03)
├── CuentasSociosTest.java             (CU04)
├── DirectivaTest.java                 (CU23)
├── PanelTest.java                     (CU25)
└── CatalogoUbicacionTest.java         (FR-004)
```

**Structure Decision**: mismo proyecto y capas de 001. Los objetos de formulario viven en
`rest.formulario` porque solo transportan datos de la vista y sus validaciones de formato; las
reglas de negocio (unicidad, límite de administradores, permisos finos) viven en `service`.
Para respetar la dependencia `rest → service` (sin dependencia inversa), cada formulario
convierte sus datos a un record del paquete `service` (`DatosJunta`, `DatosCuenta`,
`DatosConfiguracion`) antes de llamar al servicio. El catálogo de ubicación se entrega a la
vista con `CatalogoUbicacionService.mapa()`, que Thymeleaf serializa a JSON en línea; no hace
falta un método `comoJson()` aparte.

## Complexity Tracking

Sin violaciones a la constitución; no se requiere justificación.
