# Feature Specification: Módulo Gestión Administrativa

**Feature Branch**: `003-gestion-administrativa`

**Created**: 2026-11-22

**Status**: Draft

**Input**: User description: "Módulo Gestión Administrativa: casos de uso CU05 a CU08 y CU26 a CU28 de docs/modulo-2-administrativa.md, con el modelo de docs/modelo-dominio.md. Cada caso de uso es una historia de usuario; su flujo normal, alternos, excepciones y reglas son los criterios de aceptación. Mockup en docs/mockups/modulo2-CU05-CU08.png."

**Casos de uso cubiertos**: CU05, CU06, CU07, CU08, CU26, CU27, CU28.

**Depende de**: la funcionalidad 001 (sesión, matriz de permisos, aislamiento por junta,
campana de notificaciones) y la 002 (sectores de la junta, cuentas de socios, panel de la
directiva). Esta funcionalidad no redefine nada de ellas: usa las filas de la matriz ya
existentes para `/socios`, `/asistencia`, `/asistencia/correcciones`, `/predios`, `/turnos`,
`/eventos` y `/recursos`, y completa tarjetas del panel que hoy dicen "Disponible próximamente".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar y mantener la nómina de socios (Priority: P1) — CU05

El Secretario/a registra a los socios de su junta en el "Padrón de Miembros", los busca, edita
sus datos y cambia su estado.

**Why this priority**: sin socios no hay asistencia, predios, turnos ni cuentas de socio; es la
base de todo el módulo y de la 002 (CU04 usa esta nómina).

**Independent Test**: como SECRETARIO, registrar un socio válido, intentar cédulas inválidas y
repetidas, buscarlo, editarlo e inactivarlo; como TESORERO, ver la nómina sin poder cambiarla.

**Acceptance Scenarios**:

1. **Given** un SECRETARIO, **When** abre "Nómina de socios", **Then** ve el "Padrón de
   Miembros" de su junta con código, nombre, cédula, sector, celular y estado, un buscador
   por cédula o nombre y el botón "Añadir Nuevo Socio". (CU05 disparador)
2. **Given** "Añadir Nuevo Socio", **Then** el formulario muestra dos tarjetas: "Datos
   Personales" (cédula, nombres, apellidos) y "Datos de Contacto" (celular y sector como lista
   desplegable con los sectores de la junta). (CU05 flujo)
3. **Given** datos válidos, **When** presiona "Guardar Ficha de Socio", **Then** el socio queda
   registrado como ACTIVO, sin cuenta de acceso, con código de socio y fecha de ingreso de hoy,
   y la tabla se muestra con el socio nuevo arriba y el mensaje "Socio registrado". (CU05 flujo)
4. **Given** una cédula que no tiene 10 dígitos o tiene letras, **When** el secretario la
   escribe, **Then** el botón "Guardar Ficha de Socio" se deshabilita y aparece el texto de
   ayuda "La cédula debe tener 10 dígitos"; si se envía igual, el sistema la rechaza con el mismo
   mensaje. (CU05 excepción)
5. **Given** una cédula ya registrada en la plataforma, **When** guarda, **Then** se rechaza con
   "Esta cédula ya está registrada en la plataforma". (CU05 regla)
6. **Given** una junta sin sectores, **When** el secretario abre el formulario, **Then** se
   muestra "Registre al menos un sector para poder inscribir socios" y no se puede guardar.
   (CU05 regla: todo socio tiene un sector de CU03)
7. **Given** un socio, **When** el secretario edita nombres, apellidos, celular o sector y
   guarda, **Then** se actualizan sus datos; la cédula no se puede editar. (CU05 regla)
8. **Given** un socio ACTIVO o INACTIVO, **When** el secretario cambia su estado, **Then** el
   estado se actualiza; un socio INACTIVO no aparece en nuevas tomas de asistencia ni recibe
   cuenta en CU04. (CU05 regla)
9. **Given** un ADMIN_JUNTA o TESORERO, **When** abre la nómina, **Then** la ve y puede buscar,
   pero no ve los botones de añadir, editar ni cambiar estado, y cualquier intento de modificar
   responde 403.

---

### User Story 2 - Gestionar eventos (Priority: P1) — CU26

El Secretario/a crea Asambleas, Mingas y Reuniones en el Calendario de Actividades, y edita o
cancela las que todavía no tienen asistencia.

**Why this priority**: la asistencia (CU06) y las multas (módulo 3) dependen de los eventos.

**Independent Test**: crear un evento de cada tipo, editarlo, cancelarlo, y comprobar que uno
con asistencia cerrada no se puede cambiar.

**Acceptance Scenarios**:

1. **Given** un SECRETARIO en "Eventos", **When** presiona "Nuevo Evento", **Then** ve un
   formulario con tipo (Asamblea, Minga, Reunión), fecha, hora, lugar y descripción. (CU26
   flujo)
2. **Given** datos válidos, **When** guarda, **Then** el evento queda PROGRAMADO y aparece como
   tarjeta en el calendario de actividades con su tipo, fecha, hora y lugar y, si su fecha es hoy
   o pasada, el botón "Tomar Asistencia". (CU26 flujo)
3. **Given** un evento sin asistencia registrada, **When** el secretario lo edita o lo cancela,
   **Then** se guardan los cambios; un evento cancelado queda en estado CANCELADO, sigue visible
   como "Cancelado" y no permite tomar asistencia. (CU26 alterno)
4. **Given** un evento con asistencia cerrada, **When** se intenta editar o cancelar, **Then** el
   sistema lo rechaza con "No se puede modificar un evento con asistencia cerrada". (CU26
   excepción)
5. **Given** campos obligatorios vacíos (tipo, fecha, hora o lugar), **When** guarda, **Then**
   cada campo muestra "Este campo es obligatorio".

---

### User Story 3 - Registrar asistencia a eventos (Priority: P1) — CU06

El Secretario/a toma la asistencia de un evento marcando a cada socio activo como Presente,
Ausente o Justificado, y cierra el registro.

**Why this priority**: es la fuente de las multas por inasistencia del módulo financiero.

**Independent Test**: tomar asistencia de una asamblea de hoy, marcar varios socios, cerrar y
verificar el resumen, el bloqueo y la consulta de inasistencias.

**Acceptance Scenarios**:

1. **Given** un evento Asamblea o Minga PROGRAMADO con fecha de hoy o pasada, **When** el
   secretario presiona "Tomar Asistencia", **Then** ve la lista de socios ACTIVOS de su junta
   con las opciones [Presente] [Ausente] [Justificado], todos en Ausente, y un buscador por
   cédula o nombre. (CU06 flujo)
2. **Given** un evento futuro, cancelado o de tipo Reunión, **Then** no se ofrece "Tomar
   Asistencia" y la ruta directa lo rechaza con "Este evento no admite toma de asistencia".
   (CU06 precondición)
3. **Given** la lista marcada, **When** presiona "Cerrar Registro de Asistencia" y confirma,
   **Then** el sistema guarda un estado por socio, marca el evento con asistencia cerrada y
   muestra el resumen con presentes, ausentes, justificados y el porcentaje de asistencia
   (presentes sobre total). (CU06 flujo)
4. **Given** un registro cerrado, **When** alguien intenta volver a marcar o cerrar, **Then** el
   sistema lo rechaza con "El registro de asistencia ya está cerrado" y solo se puede consultar.
   (CU06 regla)
5. **Given** un socio marcado Justificado, **Then** el sistema no lo cuenta como inasistencia
   para multas. (CU06 alterno)
6. **Given** eventos con asistencia cerrada, **Then** el sistema ofrece al módulo financiero la
   lista de inasistencias (socios Ausentes, no Justificados) de cada evento cerrado de la
   junta. (CU06 regla IAsistencia)
7. **Given** un ADMIN_JUNTA o TESORERO, **When** abre "Asistencia", **Then** ve los registros y
   sus resúmenes, pero no puede marcar ni cerrar (403 si lo intenta).

---

### User Story 4 - Gestionar recursos de riego (Priority: P2) — CU27

El Secretario/a mantiene la lista de compuertas, canales y reservorios de la junta.

**Why this priority**: el calendario de turnos (CU08) necesita recursos activos.

**Independent Test**: crear recursos, intentar un nombre repetido, editar, desactivar y
reactivar, y comprobar el selector de CU08.

**Acceptance Scenarios**:

1. **Given** un SECRETARIO, **When** abre la pestaña "Recursos" del Calendario de Servicios,
   **Then** ve los recursos de su junta con nombre, tipo, sector, caudal (L/s) y estado.
   (CU27 flujo)
2. **Given** nombre, tipo (Compuerta, Canal, Reservorio), sector y caudal mayor que 0,
   **When** guarda, **Then** el recurso queda activo. (CU27 flujo)
3. **Given** un nombre que ya existe en la junta (sin distinguir mayúsculas), **When** guarda,
   **Then** se rechaza con "Ya existe un recurso con ese nombre". (CU27 excepción)
4. **Given** un recurso activo, **When** lo desactiva, **Then** deja de aparecer en el selector
   de CU08 pero sus turnos pasados se conservan y siguen visibles; puede reactivarlo. (CU27
   alterno)

---

### User Story 5 - Gestionar calendario y turnos (Priority: P2) — CU08

El Secretario/a agenda turnos de riego en una vista semanal de lunes a viernes, eligiendo
sector y recurso, y los reprograma; el sistema impide usar un recurso dos veces a la misma hora.

**Why this priority**: organiza el uso del agua, pero la junta puede operar provisionalmente sin
él.

**Independent Test**: agendar turnos en la semana, provocar un conflicto de recurso,
reprogramar, navegar entre semanas y comprobar la notificación al socio del sector.

**Acceptance Scenarios**:

1. **Given** un SECRETARIO, **When** abre "Calendario y turnos", **Then** ve la semana actual de
   lunes a viernes con los turnos como bloques de color con hora, sector y recurso, y puede
   pasar a la semana anterior o siguiente. (CU08 disparador)
2. **Given** un día y un bloque de horas (inicio y fin, fin posterior al inicio), **When** el
   secretario elige sector, recurso activo y responsable y presiona "Agendar", **Then** el turno
   aparece como bloque en ese día. (CU08 flujo)
3. **Given** un recurso que ya tiene un turno que se cruza en ese día y horario, **When** se
   agenda o reprograma otro turno con el mismo recurso, **Then** se rechaza con "Conflicto de
   recursos detectado". (CU08 excepción)
4. **Given** un turno existente, **When** el secretario cambia su día u horas y guarda, **Then**
   el turno queda reprogramado, con la misma validación de conflicto. (CU08 alterno)
5. **Given** un turno creado o reprogramado, **Then** cada socio con cuenta activa del sector
   del turno recibe la notificación "Turno de riego programado" o "Turno de riego
   reprogramado" con día, horario y recurso. (CU08 regla y CU29)
6. **Given** sábado o domingo, **Then** no se pueden agendar turnos (la vista es de lunes a
   viernes).
7. **Given** un ADMIN_JUNTA, **When** abre el calendario, **Then** lo ve sin poder agendar ni
   reprogramar.

---

### User Story 6 - Registrar predios y su ubicación en el mapa (Priority: P2) — CU07

El Secretario/a asigna predios a un socio, con área, sector, ubicación en un mapa y foto
opcional, desde el perfil del socio.

**Why this priority**: documenta los terrenos regados, pero no bloquea la operación diaria.

**Independent Test**: asignar un predio con pin y foto, intentar sin pin y con foto grande,
guardar sin foto y agregarla al editar, y ver la miniatura en el perfil.

**Acceptance Scenarios**:

1. **Given** el perfil de un socio, **When** el secretario presiona "Asignar Predio", **Then** ve
   Área (m²), Sector, un mapa para colocar el pin y una zona para subir foto. (CU07 flujo)
2. **Given** área mayor que 0, sector, pin colocado y foto JPG o PNG, **When** presiona "Guardar
   Predio", **Then** se guardan latitud, longitud y foto, el predio recibe un código y el perfil
   del socio muestra el predio con un mapa en miniatura y su foto. (CU07 flujo)
3. **Given** un predio sin foto, **When** se guarda, **Then** queda registrado y luego se puede
   editar para agregar la foto. (CU07 alterno)
4. **Given** una foto de más de 5 MB, **When** guarda, **Then** se rechaza con "El archivo
   excede el límite permitido"; un archivo que no es JPG ni PNG se rechaza con "La foto debe ser
   JPG o PNG". (CU07 excepción)
5. **Given** que no se colocó el pin, **When** guarda, **Then** se rechaza con "Coloque la
   ubicación del predio en el mapa". (CU07 regla)
6. **Given** un ADMIN_JUNTA, **When** ve el perfil de un socio, **Then** ve sus predios y mapas
   sin poder asignar ni editar.

---

### User Story 7 - Corregir asistencia cerrada (Priority: P3) — CU28

El Secretario/a solicita cambiar el estado de un socio en un registro de asistencia cerrado;
el Administrador de la Junta aprueba o rechaza, y todo queda en un historial de auditoría.

**Why this priority**: corrige errores excepcionales; el flujo normal no lo necesita.

**Independent Test**: solicitar una corrección, aprobarla como administrador, solicitar otra y
rechazarla, y revisar el historial.

**Acceptance Scenarios**:

1. **Given** un registro de asistencia cerrado, **When** el secretario presiona "Solicitar
   corrección", elige al socio, el nuevo estado y escribe el motivo, **Then** la solicitud queda
   PENDIENTE y los ADMIN_JUNTA activos de la junta reciben la notificación "Corrección de
   asistencia por autorizar". (CU28 flujo y CU29)
2. **Given** el motivo vacío o el nuevo estado igual al actual, **When** envía, **Then** se
   rechaza con "El motivo es obligatorio" o "El nuevo estado debe ser distinto del actual".
3. **Given** una solicitud PENDIENTE, **When** el ADMIN_JUNTA la aprueba, **Then** el estado de
   esa asistencia cambia, la solicitud queda APROBADA y se guarda el registro de auditoría con
   quién solicitó, quién resolvió, cuándo, estado anterior, estado nuevo y motivo; el resumen
   del evento se actualiza. (CU28 flujo)
4. **Given** una solicitud PENDIENTE, **When** el ADMIN_JUNTA la rechaza, **Then** la asistencia
   queda igual y la solicitud queda RECHAZADA en el historial. (CU28 alterno)
5. **Given** cualquier resolución, **Then** el secretario que solicitó recibe la notificación
   "Corrección de asistencia aprobada" o "Corrección de asistencia rechazada".
6. **Given** una asistencia que ya tiene una solicitud PENDIENTE, **When** se pide otra, **Then**
   se rechaza con "Ya existe una solicitud pendiente para este socio".
7. **Given** el historial de correcciones de un evento, **Then** se muestra a ADMIN_JUNTA y
   SECRETARIO y no tiene ninguna opción para editarlo ni borrarlo. (CU28 regla)
8. **Given** un SECRETARIO, **Then** no puede aprobar ni rechazar; **given** un ADMIN_JUNTA,
   **then** no puede solicitar correcciones (403 en ambos casos).

---

### Edge Cases

- Socio cambiado a INACTIVO después de cerrar una asistencia: su estado en ese registro se
  conserva.
- Socio que cambia de sector: los predios y turnos ya registrados no cambian.
- Evento con fecha de hoy pero hora futura: se permite tomar asistencia (se compara por fecha).
- Turnos que solo se tocan en el borde (uno termina 10:00 y otro empieza 10:00): no hay
  conflicto.
- Recurso desactivado con turnos futuros ya agendados: esos turnos se conservan y se muestran;
  no se pueden crear turnos nuevos con ese recurso.
- Quitar un sector en CU03 que tiene predios, recursos o turnos asignados: no se permite, igual
  que con socios ("No se puede quitar un sector con socios, predios, recursos o turnos
  asignados").
- Pin del mapa fuera del Ecuador continental e insular: se rechaza con "La ubicación debe estar
  dentro del Ecuador".
- Toma de asistencia de una junta sin socios activos: se muestra "No hay socios activos" y no se
  puede cerrar.
- Dos secretarios toman la misma asistencia a la vez: el primero que cierra gana; el segundo
  recibe "El registro de asistencia ya está cerrado".
- Datos de otra junta pedidos por identificador (socio, evento, predio, turno, recurso,
  solicitud de corrección): responden como no encontrados.

## Requirements *(mandatory)*

### Functional Requirements

**Nómina de socios (CU05)**

- **FR-001** (CU05): El SECRETARIO MUST poder registrar socios con cédula, nombres, apellidos,
  celular y sector de su junta; el socio nace ACTIVO, sin cuenta, con código de socio generado y
  fecha de ingreso de hoy.
- **FR-002** (CU05): La cédula MUST tener exactamente 10 dígitos y ser única en la plataforma
  (regla vigente desde 001/002); el botón de guardar MUST deshabilitarse mientras la cédula no
  tenga 10 dígitos y el servidor MUST validarlo igualmente.
- **FR-003** (CU05): Todo socio MUST tener un sector de su junta; sin sectores no se puede
  registrar socios.
- **FR-004** (CU05): El SECRETARIO MUST poder buscar por cédula o nombre, editar nombres,
  apellidos, celular y sector, y cambiar el estado ACTIVO/INACTIVO; la cédula no se edita.
- **FR-005** (CU05): El padrón MUST listar primero los socios registrados más recientemente.
- **FR-006** (CU05): El celular, si se ingresa, MUST tener 10 dígitos.

**Eventos (CU26)**

- **FR-007** (CU26): El SECRETARIO MUST poder crear eventos con tipo (ASAMBLEA, MINGA, REUNION),
  fecha, hora, lugar y descripción opcional; nacen PROGRAMADOS.
- **FR-008** (CU26): Un evento sin asistencia registrada MUST poder editarse o cancelarse; uno con
  asistencia cerrada MUST NOT.
- **FR-009** (CU26): El calendario de actividades MUST mostrar los eventos como tarjetas por mes,
  con el botón "Tomar Asistencia" solo cuando corresponda.

**Asistencia (CU06)**

- **FR-010** (CU06): La toma de asistencia MUST permitirse únicamente en eventos ASAMBLEA o
  MINGA, PROGRAMADOS, con fecha de hoy o anterior.
- **FR-011** (CU06): La toma MUST listar a los socios ACTIVOS de la junta, todos en AUSENTE por
  defecto, con búsqueda por cédula o nombre.
- **FR-012** (CU06): "Cerrar Registro de Asistencia" MUST guardar el estado de cada socio listado,
  marcar el evento con asistencia cerrada y mostrar presentes, ausentes, justificados y
  porcentaje de asistencia.
- **FR-013** (CU06): Un registro cerrado MUST NOT modificarse salvo por CU28.
- **FR-014** (CU06): El sistema MUST ofrecer, para uso del módulo financiero, la consulta de
  inasistencias (estado AUSENTE) de los eventos cerrados de una junta; JUSTIFICADO MUST NOT
  contar como inasistencia.

**Predios (CU07)**

- **FR-015** (CU07): El SECRETARIO MUST poder asignar predios a un socio desde su perfil, con
  área en m² mayor que 0, sector de la junta, latitud y longitud elegidas en un mapa y foto
  opcional; el predio recibe un código generado.
- **FR-016** (CU07): Un predio sin ubicación MUST NOT guardarse; la ubicación MUST estar dentro
  del Ecuador.
- **FR-017** (CU07): La foto MUST ser JPG o PNG de hasta 5 MB; si excede el tamaño se muestra
  "El archivo excede el límite permitido".
- **FR-018** (CU07): El SECRETARIO MUST poder editar un predio (incluida la foto); el perfil del
  socio MUST mostrar sus predios con mapa en miniatura y foto.

**Recursos (CU27)**

- **FR-019** (CU27): El SECRETARIO MUST poder crear y editar recursos con nombre (único en la
  junta sin distinguir mayúsculas), tipo (COMPUERTA, CANAL, RESERVORIO), sector y caudal en L/s
  mayor que 0.
- **FR-020** (CU27): El SECRETARIO MUST poder desactivar y reactivar recursos; uno inactivo MUST
  NOT ofrecerse para turnos nuevos y sus turnos MUST conservarse.

**Calendario y turnos (CU08)**

- **FR-021** (CU08): El calendario MUST mostrar una semana de lunes a viernes con navegación a
  semanas anteriores y siguientes.
- **FR-022** (CU08): El SECRETARIO MUST poder agendar turnos con fecha (lunes a viernes), hora de
  inicio y fin (fin posterior al inicio), sector, recurso activo y responsable.
- **FR-023** (CU08): Un recurso MUST NOT tener dos turnos que se crucen en la misma fecha; el
  intento se rechaza con "Conflicto de recursos detectado".
- **FR-024** (CU08): El SECRETARIO MUST poder reprogramar un turno (fecha y horas) con la misma
  validación.
- **FR-025** (CU08, CU29): Al crear o reprogramar un turno, el sistema MUST notificar a los socios
  con cuenta activa del sector del turno.

**Corrección de asistencia (CU28)**

- **FR-026** (CU28): El SECRETARIO MUST poder solicitar el cambio de estado de un socio en un
  registro cerrado con motivo obligatorio y estado distinto del actual; solo una solicitud
  PENDIENTE por asistencia.
- **FR-027** (CU28): El ADMIN_JUNTA MUST poder aprobar o rechazar solicitudes PENDIENTES de su
  junta; aprobar cambia el estado de la asistencia.
- **FR-028** (CU28): Cada solicitud MUST conservar solicitante, resolutor, fechas, estado
  anterior, estado nuevo, motivo y resultado, visibles y no editables.
- **FR-029** (CU28, CU29): El sistema MUST notificar a los ADMIN_JUNTA activos al crearse una
  solicitud y al solicitante al resolverse.

**Transversales**

- **FR-030**: Los permisos MUST seguir la matriz vigente: SECRETARIO con acceso completo;
  ADMIN_JUNTA solo lectura en socios, asistencia, predios, turnos, eventos y recursos, y acceso
  de aprobación en correcciones; TESORERO solo lectura en socios y asistencia.
- **FR-031**: Todo dato de esta funcionalidad MUST pertenecer a una junta y solo verse o
  modificarse desde esa junta; los identificadores de otra junta responden como no encontrados.
- **FR-032** (CU25): Las tarjetas del panel "Próximos eventos y turnos" (eventos PROGRAMADOS más
  turnos, ambos con fecha desde hoy hasta 7 días después) y "Asistencias pendientes de cerrar" (eventos Asamblea o Minga
  PROGRAMADOS de hoy o anteriores sin asistencia cerrada) MUST mostrar valores reales.
- **FR-033** (CU03): Un sector con predios, recursos o turnos asignados MUST NOT poder quitarse,
  además de la regla vigente sobre socios.
- **FR-034**: Todos los textos MUST estar en español y las pantallas MUST usarse en celular sin
  desplazamiento horizontal de la página.

### Key Entities *(include if feature involves data)*

- **Socio** (existente): se usan cédula, nombres, apellidos, celular, sector, código, fecha de
  ingreso y estado; se registra aquí por primera vez desde la interfaz.
- **Evento** (nuevo): tipo, fecha, hora, lugar, descripción, estado (PROGRAMADO, CANCELADO),
  asistencia cerrada; pertenece a una junta.
- **Asistencia** (nueva): estado (PRESENTE, AUSENTE, JUSTIFICADO) y observación de un socio en un
  evento.
- **CorreccionAsistencia** (nueva): estado anterior, estado nuevo, motivo, estado (PENDIENTE,
  APROBADA, RECHAZADA), fechas de solicitud y resolución, solicitante y resolutor.
- **Predio** (nuevo): código, área, sector, latitud, longitud, foto, referencia de ubicación;
  pertenece a un socio.
- **Recurso** (nuevo): nombre, tipo (COMPUERTA, CANAL, RESERVORIO), caudal, sector, activo.
- **Turno** (nuevo): fecha, hora de inicio, hora de fin, responsable, sector y recurso.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El secretario registra un socio nuevo en menos de 1 minuto.
- **SC-002**: La toma de asistencia de 200 socios (marcar, buscar y cerrar) se completa en menos
  de 10 minutos y la pantalla responde al cerrar en menos de 3 segundos.
- **SC-003**: El 100% de los intentos de agendar un recurso en un horario cruzado son
  rechazados.
- **SC-004**: El 100% de los registros de asistencia cerrados permanecen sin cambios salvo por
  correcciones aprobadas, y cada cambio tiene su registro de auditoría.
- **SC-005**: El 100% de los intentos de ver o modificar datos de otra junta son rechazados sin
  mostrar datos.
- **SC-006**: Todas las pantallas del módulo se usan sin desplazamiento horizontal en un celular
  de 360 px.

## Assumptions

- La cédula es única en toda la plataforma (decisión de la funcionalidad 002), más estricta que
  "única en la junta" de CU05.
- El mockup muestra "Nombres y apellidos" en un campo; se piden por separado porque el modelo los
  guarda así.
- El código de socio se genera con un prefijo de la junta y un número correlativo, y la fecha de
  ingreso es la del registro; ninguno se pide en el formulario porque CU05 no los menciona.
- El socio se registra sin cuenta de acceso; la cuenta se crea después en CU04.
- La toma de asistencia se hace de una sola vez y se guarda al cerrar; no hay guardado parcial.
- Las Reuniones se calendarizan pero no generan toma de asistencia ni multas (CU06 menciona solo
  Asamblea o Minga).
- La oferta de crear la convocatoria al guardar una Asamblea o Minga (regla de CU26) se agrega
  con CU15 en el módulo de comunicaciones.
- Los efectos de CU28 sobre multas (anularlas o avisar al tesorero para CU31) se agregan con el
  módulo financiero; mientras no existan multas, aprobar una corrección solo cambia la
  asistencia.
- La regla "cada socio ve resaltados los turnos de su sector" se cumple ahora con la
  notificación al socio; la vista del calendario para el socio llega con "Mi información" (CU17),
  porque la matriz de seguridad no da al socio acceso a CU08.
- El selector de recurso en CU08 lista todos los recursos activos de la junta (mostrando su
  sector); el sector del recurso indica dónde está y el del turno a quién beneficia.
- Los predios no se eliminan desde la interfaz.
- El mapa usa un servicio de mapas públicos; sin conexión a internet el mapa no carga, pero el
  resto del formulario se muestra.
