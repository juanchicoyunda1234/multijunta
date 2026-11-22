# Implementation Plan: Módulo Gestión Administrativa

**Branch**: `003-gestion-administrativa` | **Date**: 2026-11-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-gestion-administrativa/spec.md`

## Summary

Construir el módulo administrativo de la junta sobre la base de 001 y 002: nómina de socios
con perfil (CU05), eventos (CU26), toma y cierre de asistencia (CU06), recursos de riego
(CU27), calendario semanal de turnos con control de conflictos (CU08), predios con ubicación en
un mapa Leaflet y foto (CU07) y correcciones de asistencia con aprobación y auditoría (CU28).
El módulo expone la interfaz `IAsistencia` (diagrama de componentes) para que el módulo
financiero consulte inasistencias de eventos cerrados sin depender de sus entidades. Se
completan las tarjetas del panel de la directiva y la regla de quitar sectores.

## Technical Context

**Language/Version**: Java 17 (compila con JDK 21)

**Primary Dependencies**: Spring Boot 3.5.16 (web, data-jpa, security, validation, thymeleaf),
Bootstrap 5.3 e íconos por CDN, Leaflet 1.9.4 por CDN con teselas de OpenStreetMap (tech.md),
H2; sin dependencias Maven nuevas

**Storage**: H2 en archivo; tablas nuevas `evento`, `asistencia`, `correccion_asistencia`,
`predio`, `recurso`, `turno` creadas por JPA (`ddl-auto=update`); fotos en `./uploads/predios`

**Testing**: JUnit 5, Spring Boot Test, MockMvc, spring-security-test, H2 en memoria

**Target Platform**: servidor único JRE 17+; navegador de computador y celular

**Project Type**: aplicación web monolítica con vistas en el servidor

**Performance Goals**: cerrar la asistencia de 200 socios en menos de 3 s (SC-002); páginas en
menos de 1 s con volúmenes de una junta

**Constraints**: sin comentarios en el código; textos en español; responsive desde 360 px;
fotos hasta 5 MB; el mapa necesita internet (si no carga, el formulario sigue usable)

**Scale/Scope**: unas 12 pantallas (padrón, ficha, perfil, eventos, asistencia, toma,
correcciones, recursos, calendario, predio) y 7 servicios

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento en este plan | Estado |
|---|---|---|
| I. Especificación como fuente de verdad | Cada servicio, vista y regla sale de un FR de `spec.md`; mockup del módulo 2 guía las vistas | ✅ |
| II. Aislamiento por junta | `Evento`, `Recurso`, `Turno`, `Predio` y `CorreccionAsistencia` guardan su junta; `Asistencia` la hereda de su evento; repositorios con `...AndJuntaId`; prueba A/B en cada historia | ✅ |
| III. Permisos por rol | Filas de `MatrizPermisos` ya existentes (`/socios`, `/asistencia`, `/asistencia/correcciones`, `/predios`, `/turnos`, `/eventos`, `/recursos`); en correcciones el servicio distingue quién solicita (SECRETARIO) y quién resuelve (ADMIN_JUNTA) con 403 | ✅ |
| IV. Español y responsive | Bootstrap; calendario como columnas apiladas en celular; mapa con alto fijo | ✅ |
| V. Simplicidad | Sin dependencias Maven nuevas; Leaflet por CDN como dicta tech.md; calendario renderizado en el servidor | ✅ |
| VI. Código sin comentarios | Revisión en la fase de pulido | ✅ |
| VII. Verificación contra criterios | Una clase de prueba por CU con todos sus escenarios; quickstart manual | ✅ |
| VIII. Trazabilidad CUxx | Contratos, pruebas y tareas citan el CU | ✅ |

**Re-evaluación tras el diseño (Fase 1)**: sin violaciones. `IAsistencia` es una interfaz Java en
la capa `service` (no una API HTTP), por lo que no agrega superficie externa ni rompe
`rest → service → repository → model`.

## Project Structure

### Documentation (this feature)

```text
specs/003-gestion-administrativa/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── rutas-ui.md
│   ├── mensajes.md
│   └── iasistencia.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/java/ec/uta/multijunta/
├── model/
│   ├── TipoEvento.java, EstadoEvento.java, Evento.java
│   ├── EstadoAsistencia.java, Asistencia.java
│   ├── EstadoCorreccion.java, CorreccionAsistencia.java
│   ├── Predio.java
│   ├── TipoRecurso.java, Recurso.java
│   └── Turno.java
├── repository/
│   ├── EventoRepository.java, AsistenciaRepository.java, CorreccionAsistenciaRepository.java
│   ├── PredioRepository.java, RecursoRepository.java, TurnoRepository.java
│   └── SocioRepository.java          (+ búsqueda, orden por registro, socios de un sector con cuenta)
├── service/
│   ├── NominaService.java            (CU05)
│   ├── EventoService.java            (CU26)
│   ├── IAsistencia.java              (interfaz para el módulo financiero)
│   ├── Inasistencia.java             (record que devuelve IAsistencia)
│   ├── AsistenciaService.java        (CU06, implementa IAsistencia)
│   ├── CorreccionAsistenciaService.java (CU28)
│   ├── RecursoService.java           (CU27)
│   ├── TurnoService.java             (CU08)
│   ├── PredioService.java            (CU07)
│   ├── DatosSocio.java, DatosEvento.java, DatosRecurso.java, DatosTurno.java, DatosPredio.java
│   ├── ArchivoService.java           (cambia: validación de imágenes reutilizable y fotos de predios)
│   ├── ConfiguracionJuntaService.java (cambia: no quitar sectores con predios, recursos o turnos)
│   ├── PanelService.java             (cambia: tarjetas de eventos/turnos y asistencias pendientes)
│   └── SocioService.java             (cambia: genera código de socio)
├── rest/
│   ├── NominaController.java         (CU05: /socios)
│   ├── EventoController.java         (CU26: /eventos)
│   ├── AsistenciaController.java     (CU06: /asistencia)
│   ├── CorreccionAsistenciaController.java (CU28: /asistencia/correcciones)
│   ├── RecursoController.java        (CU27: /recursos)
│   ├── TurnoController.java          (CU08: /turnos)
│   ├── PredioController.java         (CU07: /predios)
│   ├── EnConstruccionController.java (cambia: quita rutas construidas)
│   └── formulario/
│       ├── SocioFormulario.java, EventoFormulario.java, RecursoFormulario.java,
│       └── TurnoFormulario.java, PredioFormulario.java, CorreccionFormulario.java
└── config/
    └── DatosPrueba.java              (cambia: recursos, eventos, turnos y predios de prueba)
src/main/resources/
├── messages.properties               (+ contracts/mensajes.md)
├── static/css/app.css                (+ calendario y mapa)
└── templates/
    ├── socios/padron.html, socios/formulario.html, socios/perfil.html
    ├── eventos/calendario.html
    ├── asistencia/lista.html, asistencia/toma.html, asistencia/detalle.html
    ├── asistencia/correcciones.html
    ├── recursos/lista.html
    ├── turnos/semana.html
    ├── predios/formulario.html
    └── fragments/mapa.html           (cabecera de Leaflet y script común)
src/test/java/ec/uta/multijunta/
├── NominaTest.java          (CU05)
├── EventoTest.java          (CU26)
├── AsistenciaTest.java      (CU06 e IAsistencia)
├── RecursoTest.java         (CU27)
├── TurnoTest.java           (CU08)
├── PredioTest.java          (CU07)
└── CorreccionAsistenciaTest.java (CU28)
```

**Structure Decision**: mismo proyecto y capas de 001/002, con el patrón de 002: los
formularios de `rest.formulario` convierten sus datos a records `Datos*` del paquete `service`.
`IAsistencia` y su record `Inasistencia` viven en `service`; el módulo financiero dependerá de
esa interfaz y no de `AsistenciaRepository` ni de `Asistencia`.

## Complexity Tracking

Sin violaciones a la constitución; no se requiere justificación.
