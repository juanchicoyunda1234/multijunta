---

description: "Lista de tareas de la funcionalidad 003-gestion-administrativa"
---

# Tasks: Módulo Gestión Administrativa

**Input**: Design documents from `/specs/003-gestion-administrativa/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: incluidas. La constitución (Principio VII) exige verificar cada CU contra sus
criterios de aceptación; `plan.md` define una clase de prueba por CU. Se escriben antes de
implementar cada historia y deben fallar primero.

**Organization**: tareas agrupadas por historia de `spec.md`; cada tarea indica su CU
(Principio VIII). Se reutiliza 001 y 002 sin duplicarlas.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia de `spec.md` (US1 a US7)

## Path Conventions

- Código: `src/main/java/ec/uta/multijunta/` (abreviado `JAVA/`)
- Recursos: `src/main/resources/` (abreviado `RES/`)
- Pruebas: `src/test/java/ec/uta/multijunta/` (abreviado `TEST/`)
- Sin comentarios en el código (Principio VI); textos en español (Principio IV)

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 [P] Agregar a `RES/messages.properties` todas las claves de `specs/003-gestion-administrativa/contracts/mensajes.md`
- [X] T002 [P] Agregar a `RES/static/css/app.css` estilos de calendario semanal (columnas lunes–viernes apilables en celular, bloques de turno), mapa (alto fijo, miniatura) y tarjetas de evento

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de terminar esta fase

- [X] T003 [P] Crear enums `JAVA/model/TipoEvento.java`, `JAVA/model/EstadoEvento.java`, `JAVA/model/EstadoAsistencia.java`, `JAVA/model/EstadoCorreccion.java` y `JAVA/model/TipoRecurso.java` con los valores de data-model.md (Enums nuevos), cada uno con etiqueta en español
- [X] T004 [P] Generalizar `JAVA/service/ArchivoService.java` (research.md R6): `validarImagen(archivo, claveTamano, claveTipo, campo)` con firma PNG/JPG y 5 MB, y `guardar(subcarpeta, prefijo, archivo, extension, anterior)` / `leer(subcarpeta, nombre)` con nombres validados; mantener el comportamiento de logos de 002 y sus pruebas en verde
- [X] T005 [P] Ampliar `TEST/Fabrica.java` con `evento(junta, tipo, fecha)`, `recurso(junta, sector, nombre)`, `turno(junta, sector, recurso, fecha, inicio, fin)`, `predio(socio, sector)` y `secretario(junta)`
- [X] T006 Cambiar en `TEST/PermisosPorRolTest.java` las pruebas `soloLecturaNoModifica` (usar `/pagos` con ADMIN_JUNTA, que tiene solo lectura) y `moduloEnConstruccion` (usar `/inventario` con TESORERO) para que usen rutas que siguen sin construir, de modo que no dependan de `/socios` ni `/predios`; el retiro de cada ruta de `JAVA/rest/EnConstruccionController.java` se hace dentro de la historia que la construye (T011, T018, T025, T031, T037, T043, T050)

**Checkpoint**: compila y las pruebas de 001 y 002 siguen en verde

---

## Phase 3: User Story 1 - Registrar y mantener la nómina (Priority: P1) 🎯 MVP — CU05

**Goal**: el SECRETARIO registra, busca, edita y activa/inactiva socios; los demás roles los ven.

**Independent Test**: `NominaTest` en verde; pasos 1 a 3 y 14 de quickstart.md.

### Tests for User Story 1 ⚠️

- [X] T007 [P] [US1] Escribir `TEST/NominaTest.java` (CU05) con HU1-1 a HU1-9 de spec.md: padrón con búsqueda y orden por registro, formulario en dos tarjetas, registro con código `S-` y fecha de hoy, cédula inválida y repetida (incluida la de un usuario de directiva), junta sin sectores, edición sin cambiar cédula, cambio de estado, celular inválido, presencia en el HTML del script que deshabilita "Guardar Ficha de Socio" (el comportamiento en el navegador se verifica en el paso 1 de quickstart.md), lectura para ADMIN_JUNTA y TESORERO con 403 al modificar, socio de otra junta → 404

### Implementation for User Story 1

- [X] T008 [P] [US1] Agregar a `JAVA/repository/SocioRepository.java` la búsqueda por junta y texto (cédula, nombres o apellidos) ordenada por id descendente, y la consulta de códigos existentes por junta
- [X] T009 [P] [US1] Crear `JAVA/service/DatosSocio.java` y `JAVA/rest/formulario/SocioFormulario.java` con validaciones de formato de data-model.md Socio.cedula, Socio.nombres, Socio.apellidos, Socio.celular, Socio.sector
- [X] T010 [US1] Crear `JAVA/service/NominaService.java` (CU05): `buscar(texto)`, `registrar(datos)` que reutiliza `CuentaFactory.validarCedulaLibre` y asigna data-model.md Socio.codigoSocio (research.md R7, vía `JAVA/service/SocioService.java`), Socio.fechaIngreso, Socio.estado, Socio.tieneCuenta, Socio.activo, Socio.username y Socio.password (aleatoria vía `PasswordService.generarTemporal()` + `codificar`, nunca mostrada); `editar(id, datos)` sin tocar Socio.cedula; `cambiarEstado(id, estado)`; sector siempre de la junta actual; id de otra junta → `RecursoNoEncontradoException`
- [X] T011 [US1] Crear `JAVA/rest/NominaController.java` con las rutas `/socios` de contracts/rutas-ui.md (padrón, nuevo, registrar, perfil, editar, estado); quitar `/socios` de `JAVA/rest/EnConstruccionController.java`
- [X] T012 [US1] Crear `RES/templates/socios/padron.html` (buscador, tabla responsive, botón "Añadir Nuevo Socio" solo para SECRETARIO), `RES/templates/socios/formulario.html` (tarjetas "Datos Personales" y "Datos de Contacto" del mockup, botón "Guardar Ficha de Socio" deshabilitado por script mientras la cédula no tenga 10 dígitos, texto de ayuda, aviso sin sectores) y `RES/templates/socios/perfil.html` (datos, estado, acciones para SECRETARIO, espacio para predios)
- [X] T013 [US1] Ejecutar `TEST/NominaTest.java` y corregir hasta que pase

**Checkpoint**: la junta tiene nómina gestionable

---

## Phase 4: User Story 2 - Gestionar eventos (Priority: P1) — CU26

**Goal**: el SECRETARIO crea, edita y cancela Asambleas, Mingas y Reuniones.

**Independent Test**: `EventoTest` en verde; paso 4 de quickstart.md.

### Tests for User Story 2 ⚠️

- [X] T014 [P] [US2] Escribir `TEST/EventoTest.java` (CU26) con HU2-1 a HU2-5 de spec.md: creación de cada tipo, calendario mensual con tarjetas y botón "Tomar Asistencia" solo cuando corresponde, edición y cancelación sin asistencia, rechazo con asistencia cerrada, obligatorios vacíos, lectura para ADMIN_JUNTA, evento de otra junta → 404

### Implementation for User Story 2

- [X] T015 [P] [US2] Crear entidad `JAVA/model/Evento.java` con data-model.md Evento.junta, Evento.tipo, Evento.fecha, Evento.hora, Evento.lugar, Evento.descripcion, Evento.estado, Evento.asistenciaCerrada y `JAVA/repository/EventoRepository.java` (por junta y mes, por id y junta)
- [X] T016 [P] [US2] Crear `JAVA/service/DatosEvento.java` y `JAVA/rest/formulario/EventoFormulario.java` con validaciones de formato de data-model.md Evento.tipo, Evento.fecha, Evento.hora, Evento.lugar, Evento.descripcion
- [X] T017 [US2] Crear `JAVA/service/EventoService.java` (CU26): `delMes(mes)`, `crear`, `editar`, `cancelar` según las transiciones de data-model.md Evento (rechazo `evento.cerrado` si Evento.asistenciaCerrada), `admiteToma(evento)` según data-model.md Evento (regla de toma)
- [X] T018 [US2] Crear `JAVA/rest/EventoController.java` y `RES/templates/eventos/calendario.html` (navegación por mes, tarjetas con tipo, fecha, hora, lugar y estado, modal "Nuevo Evento" y de edición, botón "Cancelar" con confirmación, botón "Tomar Asistencia" hacia `/asistencia/eventos/{id}` cuando `admiteToma`); quitar `/eventos` de `JAVA/rest/EnConstruccionController.java`
- [X] T019 [US2] Ejecutar `TEST/EventoTest.java` y corregir hasta que pase

---

## Phase 5: User Story 3 - Registrar asistencia a eventos (Priority: P1) — CU06

**Goal**: el SECRETARIO toma y cierra la asistencia; el módulo financiero dispone de `IAsistencia`.

**Independent Test**: `AsistenciaTest` en verde; paso 5 de quickstart.md.

**Dependency**: usa `Evento` de US2.

### Tests for User Story 3 ⚠️

- [X] T020 [P] [US3] Escribir `TEST/AsistenciaTest.java` (CU06) con HU3-1 a HU3-7 de spec.md: lista de socios ACTIVOS en Ausente, eventos que no admiten toma (futuro, cancelado, Reunión), cierre con resumen y porcentaje, rechazo al reintentar, lectura para ADMIN_JUNTA y TESORERO, junta sin socios activos; y las garantías 1 a 5 de contracts/iasistencia.md con dos juntas
- [X] T021 [P] [US3] Escribir en `TEST/AsistenciaTest.java` el caso de rendimiento de SC-002: cerrar la asistencia de 200 socios en menos de 3 s

### Implementation for User Story 3

- [X] T022 [P] [US3] Crear entidad `JAVA/model/Asistencia.java` con data-model.md Asistencia.evento, Asistencia.socio, Asistencia.fecha, Asistencia.estado, Asistencia.observacion (única por evento y socio) y `JAVA/repository/AsistenciaRepository.java` (por evento, ausencias de eventos cerrados por junta y por evento)
- [X] T023 [P] [US3] Crear `JAVA/service/IAsistencia.java` y `JAVA/service/Inasistencia.java` exactamente como contracts/iasistencia.md
- [X] T024 [US3] Crear `JAVA/service/AsistenciaService.java` (CU06) que implementa `IAsistencia`: `eventosConAsistencia()`, `planilla(eventoId)`, `cerrar(eventoId, Map<Long, EstadoAsistencia>)` en una transacción (research.md R2; rechazos `asistencia.noAdmite`, `asistencia.yaCerrada`, `asistencia.sinSocios`; marca data-model.md Evento.asistenciaCerrada), `resumen(eventoId)` y las dos consultas de `IAsistencia`
- [X] T025 [US3] Crear `JAVA/rest/AsistenciaController.java` (GET `/asistencia`, GET `/asistencia/eventos/{id}`, POST `/asistencia/eventos/{id}/cerrar`) y las vistas `RES/templates/asistencia/lista.html`, `RES/templates/asistencia/toma.html` (radios Presente/Ausente/Justificado, Ausente por defecto, buscador en el cliente, contador en vivo, confirmación al cerrar) y `RES/templates/asistencia/detalle.html` (resumen, estados, espacio para "Solicitar corrección"); quitar `/asistencia` de `JAVA/rest/EnConstruccionController.java`
- [X] T026 [US3] Ejecutar `TEST/AsistenciaTest.java` y corregir hasta que pase

**Checkpoint**: MVP del módulo (US1 a US3): socios, eventos y asistencia cerrada con IAsistencia

---

## Phase 6: User Story 4 - Gestionar recursos de riego (Priority: P2) — CU27

**Goal**: el SECRETARIO mantiene compuertas, canales y reservorios.

**Independent Test**: `RecursoTest` en verde; paso 8 de quickstart.md.

### Tests for User Story 4 ⚠️

- [X] T027 [P] [US4] Escribir `TEST/RecursoTest.java` (CU27) con HU4-1 a HU4-4 de spec.md: lista, creación, nombre duplicado sin distinguir mayúsculas, caudal 0 o negativo, desactivar y reactivar, lectura para ADMIN_JUNTA, recurso o sector de otra junta → 404

### Implementation for User Story 4

- [X] T028 [P] [US4] Crear entidad `JAVA/model/Recurso.java` con data-model.md Recurso.junta, Recurso.nombre, Recurso.tipo, Recurso.sector, Recurso.caudal, Recurso.activo y `JAVA/repository/RecursoRepository.java` (por junta, activos por junta, `existsByJuntaIdAndNombreIgnoreCase` con variante `AndIdNot`)
- [X] T029 [P] [US4] Crear `JAVA/service/DatosRecurso.java` y `JAVA/rest/formulario/RecursoFormulario.java` con validaciones de formato de data-model.md Recurso.nombre, Recurso.tipo, Recurso.sector, Recurso.caudal
- [X] T030 [US4] Crear `JAVA/service/RecursoService.java` (CU27): `listar`, `activos`, `crear`, `editar`, `cambiarEstado` con `recurso.duplicado` y sector de la junta actual
- [X] T031 [US4] Crear `JAVA/rest/RecursoController.java` y `RES/templates/recursos/lista.html` (pestaña "Recursos" junto al calendario, tabla, formulario en modal, Desactivar/Reactivar); quitar `/recursos` de `JAVA/rest/EnConstruccionController.java`
- [X] T032 [US4] Ejecutar `TEST/RecursoTest.java` y corregir hasta que pase

---

## Phase 7: User Story 5 - Gestionar calendario y turnos (Priority: P2) — CU08

**Goal**: el SECRETARIO agenda y reprograma turnos sin conflictos de recurso; se notifica a los
socios del sector.

**Independent Test**: `TurnoTest` en verde; pasos 9 y 10 de quickstart.md.

**Dependency**: usa `Recurso` de US4.

### Tests for User Story 5 ⚠️

- [X] T033 [P] [US5] Escribir `TEST/TurnoTest.java` (CU08) con HU5-1 a HU5-7 de spec.md: semana lunes–viernes y navegación, agendar, conflicto (cruce) y no conflicto (bordes que se tocan), reprogramar con validación, fin no posterior al inicio, sábado o domingo, recurso inactivo, notificaciones a socios con cuenta activa del sector (y no a otros sectores ni sin cuenta), lectura para ADMIN_JUNTA, turno o recurso de otra junta → 404

### Implementation for User Story 5

- [X] T034 [P] [US5] Crear entidad `JAVA/model/Turno.java` con data-model.md Turno.junta, Turno.fecha, Turno.horaInicio, Turno.horaFin, Turno.responsable, Turno.sector, Turno.recurso y `JAVA/repository/TurnoRepository.java` (por junta y rango de fechas, `existsConflicto` de research.md R4)
- [X] T035 [P] [US5] Crear `JAVA/service/DatosTurno.java` y `JAVA/rest/formulario/TurnoFormulario.java` con validaciones de formato de data-model.md Turno.fecha, Turno.horaInicio, Turno.horaFin, Turno.responsable, Turno.sector, Turno.recurso
- [X] T036 [US5] Agregar a `JAVA/repository/SocioRepository.java` los socios de un sector con `tieneCuenta` y `activo`, y crear `JAVA/service/TurnoService.java` (CU08): `semana(fecha)`, `agendar`, `reprogramar` con `turno.horas`, `turno.finDeSemana`, `turno.recursoInactivo`, `turno.conflicto` y notificación a los socios del sector (research.md R5)
- [X] T037 [US5] Crear `JAVA/rest/TurnoController.java` y `RES/templates/turnos/semana.html` (cinco columnas con bloques por sector y hora, "+ Agendar" por día, modal "Agendar horario" con sector, recurso, horas y responsable, "Reprogramar" en cada bloque, navegación de semanas, modal reabierto con error); quitar `/turnos` de `JAVA/rest/EnConstruccionController.java`
- [X] T038 [US5] Ejecutar `TEST/TurnoTest.java` y corregir hasta que pase

---

## Phase 8: User Story 6 - Registrar predios y su ubicación en el mapa (Priority: P2) — CU07

**Goal**: el SECRETARIO asigna predios con mapa y foto desde el perfil del socio.

**Independent Test**: `PredioTest` en verde; pasos 11 y 12 de quickstart.md.

**Dependency**: usa el perfil de socio de US1.

### Tests for User Story 6 ⚠️

- [X] T039 [P] [US6] Escribir `TEST/PredioTest.java` (CU07) con HU6-1 a HU6-6 de spec.md: formulario, guardado con ubicación y foto, sin foto y foto agregada al editar, foto de más de 5 MB y de otro tipo, sin ubicación, fuera del Ecuador, área 0, perfil con predio y miniatura, foto servida solo a la junta, lectura para ADMIN_JUNTA, TESORERO sin sección de predios en el perfil, socio o predio de otra junta → 404

### Implementation for User Story 6

- [X] T040 [P] [US6] Crear entidad `JAVA/model/Predio.java` con data-model.md Predio.junta, Predio.socio, Predio.sector, Predio.codigo, Predio.area, Predio.ubicacion, Predio.latitud, Predio.longitud, Predio.fotoNombre y `JAVA/repository/PredioRepository.java` (por socio y junta, por id y junta, códigos por junta)
- [X] T041 [P] [US6] Crear `JAVA/service/DatosPredio.java` y `JAVA/rest/formulario/PredioFormulario.java` (multipart) con validaciones de formato de data-model.md Predio.area, Predio.sector, Predio.ubicacion, Predio.latitud, Predio.longitud
- [X] T042 [US6] Crear `JAVA/service/PredioService.java` (CU07): `crear(socioId, datos)`, `editar(id, datos)`, `delSocio(socioId)`, `foto(id)`; ubicación obligatoria y dentro del Ecuador (`predio.sinUbicacion`, `predio.fueraEcuador`), código `P-` (research.md R7), foto vía `ArchivoService.validarImagen` con `predio.fotoGrande`/`predio.fotoTipo` en `./uploads/predios`
- [X] T043 [US6] Crear `RES/templates/fragments/mapa.html` (Leaflet 1.9.4 por CDN y OpenStreetMap, research.md R6), `JAVA/rest/PredioController.java` (rutas `/predios` de contracts/rutas-ui.md) y `RES/templates/predios/formulario.html` (Área, Sector, mapa con pin que llena latitud y longitud, zona de foto, "Guardar Predio"); agregar la sección de predios con miniaturas a `RES/templates/socios/perfil.html` solo para roles con acceso a `/predios`; quitar `/predios` de `JAVA/rest/EnConstruccionController.java`
- [X] T044 [US6] Traducir `MaxUploadSizeExceededException` en rutas `/predios` a `predio.fotoGrande` en `JAVA/rest/VistaComunAdvice.java` (hoy redirige siempre a la configuración de la junta)
- [X] T045 [US6] Ejecutar `TEST/PredioTest.java` y corregir hasta que pase

---

## Phase 9: User Story 7 - Corregir asistencia cerrada (Priority: P3) — CU28

**Goal**: el SECRETARIO solicita y el ADMIN_JUNTA aprueba o rechaza correcciones con auditoría.

**Independent Test**: `CorreccionAsistenciaTest` en verde; pasos 6 y 7 de quickstart.md.

**Dependency**: usa `Asistencia` de US3.

### Tests for User Story 7 ⚠️

- [X] T046 [P] [US7] Escribir `TEST/CorreccionAsistenciaTest.java` (CU28) con HU7-1 a HU7-8 de spec.md: solicitud con notificación a los ADMIN_JUNTA, motivo vacío y mismo estado, aprobación que cambia el estado, actualiza el resumen y hace desaparecer la inasistencia de `IAsistencia`, rechazo sin cambios, notificación al solicitante, una sola pendiente, historial sin rutas de edición o borrado, 403 para SECRETARIO al resolver y ADMIN_JUNTA al solicitar, solicitud o asistencia de otra junta → 404

### Implementation for User Story 7

- [X] T047 [P] [US7] Crear entidad `JAVA/model/CorreccionAsistencia.java` con data-model.md CorreccionAsistencia.junta, CorreccionAsistencia.asistencia, CorreccionAsistencia.estadoAnterior, CorreccionAsistencia.estadoNuevo, CorreccionAsistencia.motivo, CorreccionAsistencia.estado, CorreccionAsistencia.fechaSolicitud, CorreccionAsistencia.fechaResolucion, CorreccionAsistencia.solicitante, CorreccionAsistencia.resolutor y `JAVA/repository/CorreccionAsistenciaRepository.java`
- [X] T048 [P] [US7] Crear `JAVA/rest/formulario/CorreccionFormulario.java` (asistenciaId, estadoNuevo, motivo)
- [X] T049 [US7] Crear `JAVA/service/CorreccionAsistenciaService.java` (CU28) según research.md R8: `solicitar` (solo SECRETARIO, si no `AccessDeniedException`), `aprobar` y `rechazar` (solo ADMIN_JUNTA), `listar()`; transiciones de data-model.md CorreccionAsistencia y cambio de Asistencia.estado al aprobar; notificaciones de research.md R5
- [X] T050 [US7] Crear `JAVA/rest/CorreccionAsistenciaController.java` y `RES/templates/asistencia/correcciones.html` (pendientes con Aprobar/Rechazar para ADMIN_JUNTA, historial de solo lectura); agregar a `RES/templates/asistencia/detalle.html` el modal "Solicitar corrección" para SECRETARIO; quitar `/asistencia/correcciones` de `JAVA/rest/EnConstruccionController.java`
- [X] T051 [US7] Ejecutar `TEST/CorreccionAsistenciaTest.java` y corregir hasta que pase

**Checkpoint**: todas las historias funcionan

---

## Phase 10: Polish & Cross-Cutting Concerns

- [X] T052 Completar en `JAVA/service/PanelService.java` las tarjetas "Próximos eventos y turnos" y "Asistencias pendientes de cerrar" según research.md R9 y actualizar `TEST/PanelTest.java` (FR-032)
- [X] T053 Ampliar en `JAVA/service/ConfiguracionJuntaService.java` la regla de quitar sectores a predios, recursos y turnos con la clave `sector.enUso` (FR-033, data-model.md Sector) y en `TEST/ConfiguracionJuntaTest.java` actualizar la aserción existente de `quitarSectores` al texto nuevo y agregar casos con predio, recurso y turno
- [X] T054 Ampliar `JAVA/config/DatosPrueba.java` según research.md R10 sin duplicar en reinicios, y actualizar `TEST/DatosPruebaTest.java` y `README.md`
- [X] T055 Ejecutar `.\mvnw.cmd test` completo (001, 002 y 003) y dejar todo en verde
- [X] T056 [P] Revisar que ningún archivo bajo `src/` tenga comentarios (Principio VI)
- [X] T057 [P] Revisar textos en español y ausencia de desplazamiento horizontal a 360 px en las pantallas nuevas (FR-034, SC-006)
- [X] T058 Ejecutar la validación manual de `specs/003-gestion-administrativa/quickstart.md` (pasos 1 a 15) y restaurar `data/` y `uploads/` al terminar
- [X] T059 Verificar trazabilidad: cada FR de spec.md cubierto por una tarea y una prueba con su CUxx (Principio VIII)

---

## Dependencies & Execution Order

- **Setup (1)** → **Foundational (2)** → historias
- **US1 (3)** CU05: tras Foundational
- **US2 (4)** CU26: tras Foundational
- **US3 (5)** CU06: tras US2 (necesita `Evento`); usa socios (US1 o `Fabrica`)
- **US4 (6)** CU27: tras Foundational
- **US5 (7)** CU08: tras US4 (necesita `Recurso`)
- **US6 (8)** CU07: tras US1 (perfil del socio)
- **US7 (9)** CU28: tras US3 (necesita `Asistencia`)
- **Polish (10)**: tras todas; T052 necesita US2, US3 y US5; T053 necesita US4, US5 y US6

### Within Each User Story

- Prueba primero (debe fallar) → entidad y repositorio → formulario y datos → servicio →
  controlador y vistas → ejecución de pruebas

### Parallel Opportunities

- Setup: T001 y T002; Foundational: T003, T004, T005
- Tras Foundational, tres líneas en paralelo: US1→US6 (socios y predios), US2→US3→US7 (eventos,
  asistencia y correcciones) y US4→US5 (recursos y turnos)
- Dentro de cada historia, las tareas [P] de prueba, entidad y formulario

---

## Parallel Example: User Story 3

```text
Task: "Escribir TEST/AsistenciaTest.java (CU06)"
Task: "Crear JAVA/model/Asistencia.java y AsistenciaRepository"
Task: "Crear JAVA/service/IAsistencia.java e Inasistencia.java"
```

---

## Implementation Strategy

### MVP First

1. Setup + Foundational
2. US1 (CU05) → nómina operativa
3. US2 (CU26) y US3 (CU06) → asistencia cerrada e `IAsistencia` listos para el módulo financiero

### Incremental Delivery

1. MVP (US1 a US3)
2. US4 y US5 → recursos y turnos
3. US6 → predios con mapa
4. US7 → correcciones con auditoría
5. Polish → panel, regla de sectores, datos de prueba y puertas de calidad

---

## Notes

- [P] = archivos distintos, sin dependencias pendientes
- [USx] y el CUxx en la descripción dan la trazabilidad exigida por la constitución
- Detenerse en cada checkpoint para validar la historia
