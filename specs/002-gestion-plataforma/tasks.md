---

description: "Lista de tareas de la funcionalidad 002-gestion-plataforma"
---

# Tasks: Módulo Gestión de Plataforma

**Input**: Design documents from `/specs/002-gestion-plataforma/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: incluidas. La constitución (Principio VII) exige verificar cada caso de uso contra
sus criterios de aceptación y `plan.md` define una clase de prueba por CU. Se escriben antes
de implementar cada historia y deben fallar primero.

**Organization**: tareas agrupadas por historia de `spec.md`; cada tarea de historia indica su
CU (Principio VIII). Se reutiliza la funcionalidad 001 sin duplicarla.

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

**Purpose**: configuración y mensajes compartidos

- [X] T001 Agregar a `RES/application.properties` `spring.servlet.multipart.max-file-size=5MB`, `spring.servlet.multipart.max-request-size=6MB` y `multijunta.uploads=./uploads` (research.md R4)
- [X] T002 [P] Agregar a `RES/messages.properties` todas las claves de `specs/002-gestion-plataforma/contracts/mensajes.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: piezas que usan varias historias

**⚠️ CRITICAL**: ninguna historia empieza antes de terminar esta fase

- [X] T003 [P] Ampliar `JAVA/service/ReglaNegocioException.java` con un `campo` opcional (constructor `(clave, campo)` y `getCampo()`), manteniendo el constructor actual
- [X] T004 [P] Agregar a `JAVA/repository/UsuarioRepository.java` `existsByCedula`, `existsByEmailIgnoreCaseAndRolIn`, `existsByEmailIgnoreCaseAndRolInAndIdNot`, `countByJuntaIdAndRolAndActivoTrue` y `findByJuntaIdAndRolInOrderByApellidosAsc` para las reglas de data-model.md Usuario.email y Usuario.cedula (research.md R5)
- [X] T005 [P] Crear `JAVA/service/Credencial.java` (record `nombreCompleto`, `username`, `passwordTemporal`, `Serializable`) según data-model.md Credencial
- [X] T006 [P] Ampliar `TEST/Fabrica.java` con `juntaConCatalogo(nombre)` (provincia y cantón válidos del catálogo), `socioSinCuenta(junta, sector, estado)` y `directiva(rol, junta, email)`

**Checkpoint**: compila y las pruebas de 001 siguen en verde

---

## Phase 3: User Story 1 - Registrar nuevas juntas (Priority: P1) 🎯 MVP — CU01

**Goal**: el SUPER_ADMIN ve el listado de juntas como inicio y registra juntas en una ventana
emergente con provincia y cantón del catálogo.

**Independent Test**: `JuntaTest` (casos CU01) y `CatalogoUbicacionTest` en verde; pasos 1 a 3
de quickstart.md.

### Tests for User Story 1 ⚠️

- [X] T007 [P] [US1] Escribir `TEST/CatalogoUbicacionTest.java` (FR-004): 24 provincias, "Tungurahua" contiene "Ambato" y "Pelileo", par provincia–cantón inválido rechazado
- [X] T008 [P] [US1] Escribir en `TEST/JuntaTest.java` los casos CU01: HU1-1 a HU1-6 de spec.md (listado con columnas y botón, registro válido con mensaje y fecha de hoy, RUC duplicado con modal abierto y lo escrito conservado, RUC de 12/14 dígitos o con guiones, obligatorios vacíos, cantón de otra provincia); inicio del SUPER_ADMIN en `/plataforma/juntas`; `/plataforma` redirige al listado

### Implementation for User Story 1

- [X] T009 [P] [US1] Crear `RES/catalogos/ecuador-dpa.csv` (`provincia;canton`, 24 provincias y 221 cantones de la DPA del INEC) y `JAVA/model/Provincia.java` (record `nombre`, `cantones`) según data-model.md Catálogo de ubicación
- [X] T010 [US1] Crear `JAVA/service/CatalogoUbicacionService.java` (CU01): carga el CSV al arrancar en un mapa ordenado, `provincias()`, `cantones(provincia)`, `esValido(provincia, canton)` y `comoJson()` para la vista (research.md R1)
- [X] T011 [P] [US1] Agregar `existsByRuc` y `existsByRucAndIdNot` a `JAVA/repository/JuntaRepository.java` y un método que liste juntas con el número de administradores activos para el listado
- [X] T012 [P] [US1] Crear `JAVA/rest/formulario/JuntaFormulario.java` con nombre, ruc, tipo, provincia y canton y validaciones de formato de data-model.md Junta.nombre, Junta.ruc, Junta.tipo, Junta.provincia, Junta.canton (mensajes `campo.obligatorio` y `junta.rucFormato`)
- [X] T013 [US1] Crear `JAVA/service/JuntaService.java` (CU01) con `listar()` y `registrar(formulario)`: RUC único (`ReglaNegocioException("junta.rucDuplicado", "ruc")`), par provincia–cantón válido (`"junta.cantonInvalido", "canton"`), junta activa con data-model.md Junta.fechaRegistro y Junta.activa asignados por el sistema
- [X] T014 [US1] Crear `JAVA/rest/JuntaController.java` (CU01): GET `/plataforma/juntas` y POST `/plataforma/juntas` según contracts/rutas-ui.md; errores → mismo listado con `abrir=registro`, errores de campo y lo escrito; éxito → flash `junta.registrada` (research.md R2)
- [X] T015 [US1] Crear `RES/templates/plataforma/juntas.html` siguiendo `docs/mockups/CU01-registrar-juntas.png`: tabla responsive (nombre, RUC, tipo, provincia, cantón, estado, motivo, administradores activos), botón "+ Nueva Organización", modal "Registro de Nueva Junta" con selectores Tipo/Provincia/Cantón, "Guardar Organización" y "Cancelar", campo inválido en rojo, y script en línea que filtra cantones con el JSON del catálogo y reabre el modal según `abrir`
- [X] T016 [US1] Cambiar `JAVA/service/UsuarioActual.java` (`rutaInicio()` de SUPER_ADMIN → `/plataforma/juntas`), `JAVA/rest/InicioController.java` (`/plataforma` redirige al listado), `JAVA/config/MatrizPermisos.java` (fila `/plataforma` sin texto de menú) y `JAVA/rest/EnConstruccionController.java` (quitar `/plataforma/juntas`) según research.md R10
- [X] T017 [US1] Ajustar `TEST/LoginTest.java` y `TEST/PermisosPorRolTest.java` al nuevo inicio del SUPER_ADMIN y ejecutar `TEST/JuntaTest.java` y `TEST/CatalogoUbicacionTest.java` hasta que pasen

**Checkpoint**: el SUPER_ADMIN registra juntas desde su inicio

---

## Phase 4: User Story 2 - Asignar Administrador de la Junta (Priority: P1) — CU02

**Goal**: el SUPER_ADMIN crea o reasigna la cuenta del administrador de una junta y ve sus
credenciales una vez.

**Independent Test**: `AdministradorTest` en verde; pasos 4 a 7 de quickstart.md.

**Dependency**: usa el listado de US1 (T014, T015).

### Tests for User Story 2 ⚠️

- [X] T018 [P] [US2] Escribir `TEST/AdministradorTest.java` (CU02) con HU2-1 a HU2-7 de spec.md: creación con cédula como usuario y marca de cambio, credenciales en flash solo una vez, límite de 2 activos, reasignación que desactiva primero, correo duplicado (sin distinguir mayúsculas) contra ADMIN/SECRETARIO/TESORERO de cualquier junta, cédula duplicada, ingreso obligado a cambiar contraseña; borde: junta suspendida permite asignar; `reemplazadoId` de otra junta → 404 sin cambios

### Implementation for User Story 2

- [X] T019 [P] [US2] Crear `JAVA/rest/formulario/AdministradorFormulario.java` con cedula, nombres, apellidos, email y reemplazadoId opcional, con validaciones de formato de data-model.md Usuario.cedula, Usuario.nombres, Usuario.apellidos, Usuario.email (correo obligatorio solo en este formulario, no en la entidad)
- [X] T020 [US2] Crear `JAVA/service/AdministradorService.java` (CU02): `asignar(juntaId, formulario)` y `reasignar(juntaId, reemplazadoId, formulario)` en una transacción según research.md R6; valida data-model.md Usuario.cedula (`cuenta.cedulaDuplicada`) y Usuario.email (`cuenta.correoDuplicado`, guardado en minúsculas), límite de 2 por la regla de conteo de data-model.md Usuario (`admin.limite`); crea con data-model.md Usuario.rol = ADMIN_JUNTA, Usuario.username = cédula, Usuario.junta = junta destino, Usuario.activo y Usuario.debeCambiarPassword = true; contraseña con `PasswordService.generarTemporal()`; devuelve `Credencial`
- [X] T021 [US2] Agregar a `JAVA/rest/JuntaController.java` POST `/plataforma/juntas/{id}/administradores` según contracts/rutas-ui.md: error → listado con `abrir=administrador` y la junta destino; éxito → flash con la `Credencial`, el nombre y RUC de la junta destino (para el resumen de HU2-2) y `admin.creado`
- [X] T022 [US2] Agregar a `RES/templates/plataforma/juntas.html` el ícono de llave por fila y el panel lateral (offcanvas) de `docs/mockups/CU02-asignar-administrador.png` con "Junta destino: nombre (RUC)", Cédula, Nombres, Apellidos, Correo, selector opcional "Reasignar a" con los administradores activos, botón "Generar Cuenta y Asignar Rol" y el recuadro de credenciales mostrado una vez
- [X] T023 [US2] Ejecutar `TEST/AdministradorTest.java` y corregir hasta que pase

**Checkpoint**: cada junta puede tener su administrador

---

## Phase 5: User Story 3 - Configurar datos generales y sectores (Priority: P1) — CU03

**Goal**: el ADMIN_JUNTA completa los datos de su junta y sus sectores en un formulario con
pestañas.

**Independent Test**: `ConfiguracionJuntaTest` en verde; pasos 8 y 9 de quickstart.md.

### Tests for User Story 3 ⚠️

- [X] T024 [P] [US3] Escribir `TEST/ConfiguracionJuntaTest.java` (CU03) con HU3-1 a HU3-9 de spec.md: pestañas y campos de solo lectura, guardado completo, dirección vacía con `pestana=ubicacion` y foco, sectores nuevos, vacíos y duplicados sin distinguir mayúsculas, quitar sector con socios rechazado, logo no imagen o mayor de 5 MB rechazado, monto negativo o con 3 decimales rechazado, `/logo/{juntaId}` de otra junta → 404, la junta B no cambia al guardar la A, aviso cuando la junta no tiene sectores

### Implementation for User Story 3

- [X] T025 [P] [US3] Agregar a `JAVA/repository/SectorRepository.java` `existsByJuntaIdAndNombreIgnoreCase` y a `JAVA/repository/SocioRepository.java` `existsBySectorId` para las reglas de data-model.md Sector.nombre y Sector.junta
- [X] T026 [P] [US3] Crear `JAVA/rest/formulario/ConfiguracionFormulario.java` con telefono, direccion, montoMultaInasistencia, logo (MultipartFile), sectoresNuevos y sectoresQuitados, con validaciones de formato de data-model.md Junta.telefono, Junta.direccion, Junta.montoMultaInasistencia
- [X] T027 [P] [US3] Crear `JAVA/service/ArchivoService.java` (CU03): guarda logos PNG/JPG en `./uploads/logos/junta-{id}-{uuid}.{ext}`, borra el anterior y devuelve el nombre para data-model.md Junta.logoUrl; rechaza otros tipos con `configuracion.logoInvalido` (research.md R4)
- [X] T028 [US3] Crear `JAVA/service/ConfiguracionJuntaService.java` (CU03): `obtener()` y `actualizar(formulario)` en una transacción solo sobre la junta de `UsuarioActual.juntaId()`: dirección obligatoria (`configuracion.direccionObligatoria`, campo `direccion`), monto (`configuracion.montoInvalido`), alta de sectores (`sector.vacio`, `sector.duplicado`), baja de sectores de su junta sin socios (`sector.conSocios`; id de otra junta → `RecursoNoEncontradoException`) y logo vía `ArchivoService`
- [X] T029 [US3] Crear `JAVA/rest/ConfiguracionJuntaController.java` (GET/POST `/junta/configuracion`) que ante `ReglaNegocioException` vuelve a mostrar el formulario con `pestana` según el campo y lo escrito, y `JAVA/rest/LogoController.java` (GET `/logo/{juntaId}`, solo su junta o SUPER_ADMIN; si no, 404); quitar `/junta/configuracion` de `JAVA/rest/EnConstruccionController.java`
- [X] T030 [US3] Crear `RES/templates/junta/configuracion.html` siguiendo `docs/mockups/CU03-configurar-datos.png`: un formulario multipart con pestañas "Información General", "Ubicación" y "Sectores", campos de solo lectura, campo "Agregar sector..." con "+ Añadir", lista con [X], aviso `sector.ninguno`, botón "Actualizar Datos" y script en línea que añade y quita ítems con inputs ocultos, valida vacíos y duplicados y activa la pestaña y el foco indicados
- [X] T031 [US3] Agregar el logo de la junta a la barra en `RES/templates/fragments/layout.html` y el atributo `logoJunta` en `JAVA/rest/VistaComunAdvice.java`, y traducir `MaxUploadSizeExceededException` al mensaje `configuracion.logoInvalido` (FR-015)
- [X] T032 [US3] Ejecutar `TEST/ConfiguracionJuntaTest.java` y corregir hasta que pase

**Checkpoint**: MVP de plataforma (US1 a US3): juntas registradas, con administrador y configuradas

---

## Phase 6: User Story 4 - Generar accesos de socios (Priority: P2) — CU04

**Goal**: el ADMIN_JUNTA crea en bloque las cuentas de socios y las desactiva o reactiva.

**Independent Test**: `CuentasSociosTest` en verde; pasos 10 y 11 de quickstart.md.

### Tests for User Story 4 ⚠️

- [X] T033 [P] [US4] Escribir `TEST/CuentasSociosTest.java` (CU04) con HU4-1 a HU4-8 de spec.md: badges, creación en bloque con contraseñas distintas y marca de cambio, lista solo una vez, socio INACTIVO ignorado aunque se envíe su id, selección vacía, desactivar cierra el ingreso, reactivar conserva la contraseña; borde: socio de otra junta → 404 sin cambios; 50 socios en una operación (SC-003)

### Implementation for User Story 4

- [X] T034 [P] [US4] Crear `JAVA/model/EstadoCuenta.java` (SIN_CUENTA, ACTIVA, DESACTIVADA con etiquetas) y el método derivado en `JAVA/model/Socio.java` según data-model.md Socio (tabla de estado de cuenta), usando data-model.md Socio.tieneCuenta y Usuario.activo
- [X] T035 [US4] Crear `JAVA/service/CuentaSocioService.java` (CU04): `listar()` de la junta actual, `crearCredenciales(List<Long>)` que solo procesa socios de su junta, estado ACTIVO en la nómina y SIN_CUENTA, asigna data-model.md Usuario.username = cédula, Socio.tieneCuenta, Usuario.activo y Usuario.debeCambiarPassword = true con contraseña propia y devuelve la lista de `Credencial`; `desactivar(id)` y `reactivar(id)` (research.md R8); lista vacía → `socios.seleccionVacia`
- [X] T036 [US4] Crear `JAVA/rest/UsuariosController.java` (CU04): GET `/usuarios` → `/usuarios/socios`, GET `/usuarios/socios`, POST `/usuarios/socios/credenciales` → `/usuarios/credenciales` con flash, GET `/usuarios/credenciales`, POST desactivar y reactivar según contracts/rutas-ui.md; quitar `/usuarios` de `JAVA/rest/EnConstruccionController.java`
- [X] T037 [P] [US4] Crear `RES/templates/usuarios/socios.html` siguiendo `docs/mockups/CU04-cuentas-individuales.png` (pestañas Socios/Directiva, casilla de cabecera, casillas bloqueadas para INACTIVOS y cuentas existentes, badges, contador "X de Y socios seleccionados", botón "Crear Credenciales (X seleccionados)" deshabilitado sin selección, acciones Desactivar/Reactivar) y `RES/templates/usuarios/credenciales.html` (tabla imprimible con botón "Imprimir" y estilos de impresión)
- [X] T038 [US4] Ejecutar `TEST/CuentasSociosTest.java` y corregir hasta que pase

**Checkpoint**: los socios pueden recibir acceso

---

## Phase 7: User Story 5 - Gestionar cuentas de la directiva (Priority: P2) — CU23

**Goal**: el ADMIN_JUNTA administra las cuentas de Secretario/a y Tesorero/a de su junta.

**Independent Test**: `DirectivaTest` en verde; paso 12 de quickstart.md.

**Dependency**: comparte la pantalla Usuarios de US4 (T036, T037).

### Tests for User Story 5 ⚠️

- [X] T039 [P] [US5] Escribir `TEST/DirectivaTest.java` (CU23) con HU5-1 a HU5-7 de spec.md: listado, creación con contraseña temporal una vez, edición, cambio de rol que rige en la siguiente acción (el menú cambia sin nuevo ingreso), desactivar y reactivar, cédula y correo duplicados, rol ADMIN_JUNTA o SUPER_ADMIN → 403, editar o desactivar una cuenta ADMIN_JUNTA → 403, cuenta de otra junta → 404

### Implementation for User Story 5

- [X] T040 [P] [US5] Crear `JAVA/rest/formulario/DirectivaFormulario.java` con cedula, nombres, apellidos, email y rol, con validaciones de formato de data-model.md Usuario.cedula, Usuario.nombres, Usuario.apellidos, Usuario.email (obligatorio solo en este formulario), Usuario.rol
- [X] T041 [US5] Crear `JAVA/service/DirectivaService.java` (CU23): `listar()`, `crear`, `editar`, `desactivar`, `reactivar` solo sobre la junta actual; data-model.md Usuario.rol limitado a SECRETARIO/TESORERO y cuentas objetivo SECRETARIO/TESORERO (si no, `AccessDeniedException` → 403); unicidad de data-model.md Usuario.cedula y Usuario.email; creación con data-model.md Usuario.username = cédula, Usuario.junta = junta actual, Usuario.debeCambiarPassword = true y `Credencial`; id de otra junta → `RecursoNoEncontradoException`
- [X] T042 [US5] Agregar a `JAVA/rest/UsuariosController.java` las rutas `/usuarios/directiva` de contracts/rutas-ui.md y crear `RES/templates/usuarios/directiva.html` (lista con estado, formulario "Nueva cuenta" y edición en ventana emergente, selector de rol solo SECRETARIO/TESORERO, acciones Desactivar/Reactivar, ADMIN_JUNTA sin acciones, contraseña temporal mostrada una vez)
- [X] T043 [US5] Cambiar `JAVA/config/EstadoCuentaFilter.java` para que, si el rol en la base difiere del de la sesión, reemplace la autenticación por una con el rol actual (FR-026, research.md R11)
- [X] T044 [US5] Ejecutar `TEST/DirectivaTest.java` y corregir hasta que pase

**Checkpoint**: la directiva completa tiene cuentas

---

## Phase 8: User Story 6 - Editar o suspender juntas (Priority: P2) — CU24

**Goal**: el SUPER_ADMIN corrige, suspende con motivo y reactiva juntas.

**Independent Test**: casos CU24 de `JuntaTest` en verde; paso 13 de quickstart.md.

**Dependency**: usa `JuntaService`, `JuntaController` y la vista de US1.

### Tests for User Story 6 ⚠️

- [X] T045 [P] [US6] Agregar a `TEST/JuntaTest.java` los casos CU24 (HU6-1 a HU6-6 de spec.md): edición con modal precargado y mensaje, RUC de otra junta rechazado y RUC propio aceptado, suspensión con motivo obligatorio, usuarios de la junta suspendida sin ingreso y con sesión cerrada, datos conservados, reactivación que borra el motivo y devuelve el ingreso, ninguna ruta de eliminación (DELETE y POST `/eliminar` → 404 o 405)

### Implementation for User Story 6

- [X] T046 [US6] Agregar a `JAVA/service/JuntaService.java` (CU24) `editar(id, formulario)` (RUC con `existsByRucAndIdNot`, catálogo), `suspender(id, motivo)` y `reactivar(id)` según las transiciones de data-model.md Junta.activa y Junta.motivoSuspension (`junta.motivoObligatorio`)
- [X] T047 [US6] Agregar a `JAVA/rest/JuntaController.java` POST `/plataforma/juntas/{id}`, `/suspender` y `/reactivar` con los mensajes de contracts/mensajes.md y `abrir=edicion` o `abrir=suspension` ante error
- [X] T048 [US6] Agregar a `RES/templates/plataforma/juntas.html` el ícono de lápiz que abre el mismo modal con datos cargados y el modal de suspensión con motivo y confirmación, y el botón "Reactivar" con confirmación para juntas suspendidas
- [X] T049 [US6] Ejecutar `TEST/JuntaTest.java` completo y corregir hasta que pase

**Checkpoint**: administración completa de juntas

---

## Phase 9: User Story 7 - Panel de inicio de la directiva (Priority: P3) — CU25

**Goal**: la directiva ve tarjetas de resumen según su rol.

**Independent Test**: `PanelTest` en verde; paso 14 de quickstart.md.

### Tests for User Story 7 ⚠️

- [X] T050 [P] [US7] Escribir `TEST/PanelTest.java` (CU25) con HU7-1 a HU7-5 de spec.md: tarjetas exactas por rol, socios activos y solicitudes pendientes con valores reales, "Disponible próximamente" en las demás, enlaces de cada tarjeta, números solo de la junta del usuario

### Implementation for User Story 7

- [X] T051 [P] [US7] Agregar `countByJuntaIdAndEstado` a `JAVA/repository/SocioRepository.java` y crear el record `Tarjeta` en `JAVA/service/PanelService.java` según data-model.md Tarjeta del panel
- [X] T052 [US7] Implementar `JAVA/service/PanelService.java` (CU25) con las 8 definiciones y su filtro por `MatrizPermisos` según research.md R9
- [X] T053 [US7] Crear `JAVA/rest/PanelController.java` para GET `/panel` (mover el método de `JAVA/rest/InicioController.java`) y reemplazar `RES/templates/inicio/directiva.html` por la grilla responsive de tarjetas enlazadas
- [X] T054 [US7] Ejecutar `TEST/PanelTest.java` y corregir hasta que pase

**Checkpoint**: todas las historias funcionan

---

## Phase 10: Polish & Cross-Cutting Concerns

- [X] T055 Ejecutar `.\mvnw.cmd test` completo (001 y 002) y dejar todo en verde
- [X] T056 [P] Revisar que ningún archivo bajo `src/` tenga comentarios (Principio VI)
- [X] T057 [P] Revisar textos en español y ausencia de desplazamiento horizontal a 360 px en las pantallas nuevas (FR-036, SC-007)
- [X] T058 Actualizar `README.md` con las pantallas nuevas y con que las cuentas nuevas usan la cédula como usuario
- [X] T059 Ejecutar la validación manual de `specs/002-gestion-plataforma/quickstart.md` (pasos 1 a 15) y restaurar los datos de prueba borrando `data/` al terminar
- [X] T060 Verificar trazabilidad: cada FR de spec.md cubierto por una tarea y una prueba con su CUxx (Principio VIII)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (1)** → **Foundational (2)** → historias
- **US1 (3)**: tras Foundational
- **US2 (4)**: tras US1 (misma vista y controlador)
- **US3 (5)**: tras Foundational; independiente de US1/US2
- **US4 (6)**: tras Foundational
- **US5 (7)**: tras US4 (comparte `UsuariosController` y la pantalla Usuarios)
- **US6 (8)**: tras US1 (comparte servicio, controlador y vista de juntas); conviene tras US2 por la vista
- **US7 (9)**: tras Foundational; independiente
- **Polish (10)**: tras todas

### Within Each User Story

- Prueba primero (debe fallar) → formularios y repositorios → servicio → controlador y vista →
  ejecución de pruebas

### Parallel Opportunities

- Setup: T001 y T002
- Foundational: T003 a T006
- Tras Foundational pueden avanzar en paralelo tres líneas: US1→US2→US6 (juntas), US3
  (configuración) y US4→US5 (usuarios); US7 en cualquier momento
- Dentro de cada historia, las tareas [P] de prueba, formulario y repositorio

---

## Parallel Example: User Story 3

```text
Task: "Escribir TEST/ConfiguracionJuntaTest.java (CU03)"
Task: "Crear JAVA/rest/formulario/ConfiguracionFormulario.java"
Task: "Crear JAVA/service/ArchivoService.java"
Task: "Agregar consultas a SectorRepository y SocioRepository"
```

---

## Implementation Strategy

### MVP First

1. Setup + Foundational
2. US1 (CU01) → validar pasos 1 a 3
3. US2 (CU02) y US3 (CU03) → una junta nueva queda operativa con administrador y sectores

### Incremental Delivery

1. MVP (US1 + US2 + US3)
2. US4 (CU04) y US5 (CU23) → cuentas de socios y directiva
3. US6 (CU24) → suspensión
4. US7 (CU25) → panel
5. Polish

---

## Notes

- [P] = archivos distintos, sin dependencias pendientes
- [USx] y el CUxx en la descripción dan la trazabilidad exigida por la constitución
- Detenerse en cada checkpoint para validar la historia

---

## Phase 11: Convergence

- [X] T061 Dar mensajes propios a los límites de longitud: en `JAVA/service/ConfiguracionJuntaService.java` un sector de más de 80 caracteres hoy responde "Ese sector ya existe" (usar una clave nueva, p. ej. `sector.largo` = "El nombre del sector admite hasta 80 caracteres", según data-model.md Sector.nombre) y en `JAVA/service/JuntaService.java` un motivo de más de 300 caracteres responde "Indique el motivo de la suspensión" (usar `junta.motivoLargo`, según data-model.md Junta.motivoSuspension); agregar las claves a `RES/messages.properties` y `contracts/mensajes.md` y un caso a `TEST/ConfiguracionJuntaTest.java` y `TEST/JuntaTest.java` per FR-017, FR-028 (partial)
- [X] T062 Verificar con la aplicación en marcha (no con MockMvc) que subir un logo de más de 5 MB en `/junta/configuracion` muestra "El logo debe ser una imagen PNG o JPG de hasta 5 MB" en vez de cortar la conexión; si Tomcat corta la petición, ajustar `server.tomcat.max-swallow-size` en `RES/application.properties` per FR-015, HU3-7 (partial)
- [X] T063 Completar en el navegador los pasos 5, 6, 7, 11, 12 y 13 de `specs/002-gestion-plataforma/quickstart.md` (límite de administradores, reasignar, ingreso del administrador nuevo, desactivar/reactivar socio, cuenta de directiva y suspensión de junta), que T059 solo cubrió con pruebas automáticas, y restaurar `data/` al terminar per T059 (partial)
- [X] T064 Documentar en `specs/002-gestion-plataforma/plan.md` (Project Structure) los componentes agregados al implementar para respetar las capas `rest → service`: records `DatosJunta`, `DatosCuenta`, `DatosConfiguracion`, la clase `CuentaFactory`, el formulario base `CuentaFormulario`, y que `CatalogoUbicacionService.mapa()` reemplaza a `comoJson()` per plan: Project Structure (unrequested)
