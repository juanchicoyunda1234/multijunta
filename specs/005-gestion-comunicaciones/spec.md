# Feature Specification: Módulo Gestión de Comunicaciones

**Feature Branch**: `005-gestion-comunicaciones`

**Created**: 2026-11-22

**Status**: Draft

**Input**: User description: "Módulo Gestión de Comunicaciones: casos de uso CU15 a CU19, CU29 y CU32 de docs/modulo-4-comunicaciones.md, con el modelo de docs/modelo-dominio.md. Cada caso de uso es una historia de usuario; su flujo normal, alternos, excepciones y reglas son los criterios de aceptación. Mockup en docs/mockups/modulo4-CU15-CU19.png."

**Casos de uso cubiertos**: CU15, CU16, CU17, CU18, CU19, CU29 (orígenes que faltaban), CU32.

**Depende de**:
- **001**: sesión, matriz de permisos, aislamiento por junta y la campana de notificaciones con
  lista, apertura y "Marcar todas como leídas". Esa base no se redefine aquí.
- **002**: cuentas de socios (CU04) y panel de la directiva.
- **003**: socios, sectores, eventos, predios con mapa y turnos.
- **004**: multas, cuotas, pagos y estado de cuenta.

Usa las filas de la matriz que ya existen para `/convocatorias`, `/actas`, `/comunicaciones`,
`/mi-informacion`, `/anuncios` y `/mensajes`. Hoy todas muestran "Módulo en construcción", salvo
`/mi-informacion`, que muestra una página provisional. También completa la tarjeta "Mensajes no
leídos" del panel.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar información individual (Priority: P1) — CU17

El socio con cuenta inicia sesión y ve "Mi Panel" con su resumen, su estado de cuenta, su predio
y su calendario, todo de solo lectura.

**Why this priority**: es la única pantalla del socio y la razón de que tenga cuenta; prioridad
alta.

**Independent Test**: iniciar sesión como socio con deuda y pagos, revisar cada pestaña y
comprobar que no hay acciones de guardar ni eliminar y que no ve datos de otros socios.

**Acceptance Scenarios**:

1. **Given** un socio con cuenta activa, **When** inicia sesión, **Then** llega a "Mi Panel".
   El menú tiene "Resumen", "Estado de Cuenta", "Mi Predio", "Calendario", "Anuncios",
   "Repositorio" y "Mensajes". (CU17 disparador y flujo, CU32 disparador)
2. **Given** "Resumen", **Then** el socio ve:
   - su ficha: nombre, cédula, código, sector y estado;
   - su deuda total;
   - su próximo turno;
   - las convocatorias publicadas más recientes, con las Asambleas fijadas arriba. (CU17
     flujo, CU15 flujo)
3. **Given** "Estado de Cuenta", **Then** ve:
   - su "Deuda Total" en grande, en verde si es cero y en rojo si debe;
   - el detalle de multas y cuotas pendientes o parciales con su saldo;
   - la tabla de sus últimos 10 pagos (número, fecha, concepto, monto y "Reversado" si aplica);
   - un botón "Imprimir estado de cuenta". (CU17 flujo, mockup)
4. **Given** "Mi Predio", **Then** ve sus predios en el mapa con código, área, sector y foto.
   Si no tiene predios, ve "No tiene predios registrados". (CU17 alterno)
5. **Given** "Calendario", **Then** ve los turnos de su sector desde hoy hasta 4 semanas
   después, con fecha, horas, recurso y responsable. (CU17 alterno)
6. **Given** cualquier pestaña, **Then** no hay botones de guardar, editar ni eliminar.
   Cualquier intento de modificar algo responde 403. (CU17 regla)
7. **Given** credenciales incorrectas, **Then** se muestra "Usuario o contraseña inválidos"
   (vigente desde 001). (CU17 excepción)
8. **Given** un usuario de la directiva, **When** intenta abrir "Mi información", **Then**
   recibe 403.

---

### User Story 2 - Emitir convocatorias (Priority: P1) — CU15

El Secretario/a redacta convocatorias a Asambleas y Mingas. Puede publicarlas en el momento o
programarlas para una fecha futura, y los socios las reciben en su panel y en la campana.

**Why this priority**: es la comunicación oficial de la junta con sus socios; prioridad alta.

**Independent Test**: como SECRETARIO, publicar una convocatoria urgente y programar otra para
mañana, intentar una con fecha pasada, y comprobar que los socios con cuenta reciben la
notificación y ven la Asamblea fijada arriba.

**Acceptance Scenarios**:

1. **Given** un SECRETARIO en "Convocatorias", **When** presiona "Redactar Convocatoria",
   **Then** ve el formulario del mockup:
   - título;
   - tipo (Asamblea, Minga);
   - fecha y hora del evento;
   - lugar;
   - orden del día;
   - prioridad: "Urgente" publica de inmediato, "Normal" pide una fecha de publicación.
   (CU15 disparador y flujo, mockup)
2. **Given** datos válidos y prioridad Urgente, **When** presiona "Publicar Inmediatamente",
   **Then**:
   - la convocatoria queda PUBLICADA con la fecha de publicación de ahora;
   - aparece en el panel y en "Anuncios" de todos los socios;
   - cada socio con cuenta activa recibe en la campana "Nueva convocatoria", con enlace a
     "Anuncios". (CU15 flujo, CU29 origen)
3. **Given** prioridad Normal y una fecha de publicación futura, **When** guarda, **Then**:
   - la convocatoria queda en BORRADOR, visible solo para la directiva con su fecha programada;
   - al llegar esa fecha el sistema la publica solo y notifica como en el escenario 2. (CU15
     alterno)
4. **Given** una fecha del evento igual o anterior al momento actual, **Then** se rechaza con
   "La fecha debe ser futura". Una fecha de publicación pasada o posterior a la del evento se
   rechaza con "La fecha de publicación debe ser futura y anterior al evento". (CU15 excepción)
5. **Given** una convocatoria a Asamblea publicada, **Then** aparece fijada arriba en "Anuncios"
   y en el panel del socio hasta la fecha del evento. Después pasa a su lugar por fecha.
   (CU15 regla)
6. **Given** un BORRADOR, **When** el secretario lo edita o lo elimina, **Then** el cambio se
   guarda. Una convocatoria PUBLICADA no se edita ni se elimina.
7. **Given** que el secretario guarda una Asamblea o Minga en "Eventos" (CU26), **Then** el
   sistema le ofrece "Crear convocatoria", con el formulario precargado con el tipo, la fecha,
   la hora y el lugar del evento. (regla de CU26 pendiente desde 003)
8. **Given** un ADMIN_JUNTA, **When** abre "Convocatorias", **Then** las ve todas, incluidos los
   borradores, sin poder crear, editar ni eliminar. Los intentos responden 403.

---

### User Story 3 - Consultar anuncios y repositorio (Priority: P1) — CU32

El socio ve las convocatorias publicadas en "Anuncios" y las actas públicas en "Repositorio".

**Why this priority**: completa lo que el socio recibe de la directiva y es la cara pública de
CU15 y CU16.

**Independent Test**: como socio, ver anuncios con una Asamblea fijada y una Minga, abrir y
descargar un acta pública, y comprobar que un acta privada y un borrador no aparecen.

**Acceptance Scenarios**:

1. **Given** convocatorias publicadas, **When** el socio abre "Anuncios", **Then** las ve con
   título, tipo, fecha y hora del evento, orden del día y fecha de publicación. Las Asambleas
   cuyo evento no ha pasado van arriba; el resto, de la más reciente a la más antigua. (CU32
   flujo)
2. **Given** actas públicas, **When** abre "Repositorio", **Then** ve nombre, fecha de reunión,
   tipo y descripción, y puede abrir o descargar el PDF. (CU32 flujo)
3. **Given** que no hay convocatorias publicadas o actas públicas, **Then** la sección
   correspondiente muestra "No hay publicaciones por ahora". (CU32 excepción)
4. **Given** actas solo para la directiva o convocatorias en borrador, **Then** no aparecen.
   Abrir su dirección directa responde 404. (CU32 regla)
5. **Given** cualquier pantalla de Anuncios o Repositorio, **Then** no tiene acciones de
   modificar. (CU32 regla)

---

### User Story 4 - Enviar y recibir mensajes (Priority: P2) — CU18

Los socios conversan con la directiva y los miembros de la directiva conversan con los socios y
entre ellos, en una vista dividida de contactos y conversación.

**Why this priority**: canal directo para consultas; prioridad media.

**Independent Test**: como socio, escribir a "Directiva" con texto e imagen. Luego, como
ADMIN_JUNTA, ver el contador de no leídos, responder y comprobar que el socio recibe la
respuesta. Intentar escribir a otro socio o a otra junta.

**Acceptance Scenarios**:

1. **Given** cualquier usuario con acceso, **When** presiona "Mensajes" en la barra superior,
   **Then** ve una vista dividida:
   - a la izquierda, sus contactos con buscador, el último mensaje y el número de no leídos;
   - a la derecha, la conversación elegida en burbujas, las propias en su color y alineadas a
     la derecha. (CU18 disparador y flujo, mockup)
2. **Given** un socio, **Then** su único contacto es "Directiva". **When** escribe y presiona
   "Enviar", **Then**:
   - el mensaje aparece en su conversación;
   - todos los miembros activos de la directiva de su junta ven aumentado su contador de no
     leídos;
   - cada uno recibe en la campana "Nuevo mensaje". (CU18 flujo, CU29 origen)
3. **Given** un miembro de la directiva (ADMIN_JUNTA, SECRETARIO o TESORERO), **Then** sus
   contactos son:
   - la conversación "Directiva" de cada socio que haya escrito o al que la directiva haya
     escrito;
   - el resto de los socios con cuenta activa de su junta;
   - los demás miembros activos de la directiva.

   **When** responde en la conversación de un socio, **Then** el socio la ve como mensaje de
   "Directiva", con el nombre de quien respondió, y recibe la notificación. (CU18 flujo)
4. **Given** el texto vacío y ninguna imagen, **Then** el botón "Enviar" queda deshabilitado, y
   un envío forzado se rechaza con "Escriba un mensaje". (CU18 excepción)
5. **Given** una imagen JPG o PNG de hasta 5 MB adjunta, **When** envía, **Then** la imagen se
   muestra como miniatura en la burbuja y se abre en grande al pulsarla. Otros formatos o
   tamaños se rechazan. (CU18 alterno)
6. **Given** un socio, **When** intenta escribir a otro socio o a un usuario de otra junta,
   **Then** se rechaza (403 o 404). Un miembro de la directiva tampoco puede escribir a
   usuarios de otra junta. (CU18 regla)
7. **Given** una conversación abierta, **Then** los mensajes recibidos en ella se marcan como
   leídos y el contador baja. El contador de "Mensajes" y la conversación abierta se actualizan
   al recargar y cada 15 segundos sin recargar. (CU18 regla)
8. **Given** el panel de la directiva, **Then** la tarjeta "Mensajes no leídos" muestra el
   número real. (CU25)

---

### User Story 5 - Archivar y publicar actas (Priority: P2) — CU16

El Secretario/a sube las actas en PDF y decide si se publican en el portal de socios.

**Why this priority**: respaldo documental de las reuniones; prioridad media.

**Independent Test**: como SECRETARIO, subir un acta pública y otra solo para la directiva,
intentar un archivo no PDF, y comprobar qué ven el tesorero y el socio.

**Acceptance Scenarios**:

1. **Given** un SECRETARIO en "Actas", **When** presiona "Subir Documento", **Then** ve:
   - nombre del acta;
   - fecha de reunión;
   - tipo de sesión (Asamblea, Minga, Reunión);
   - descripción;
   - archivo PDF;
   - la casilla "Público en Portal Socios", marcada por defecto. (CU16 disparador y flujo,
     mockup)
2. **Given** datos válidos, **When** presiona "Guardar en Archivo", **Then** el acta aparece en
   la lista de actas con su fecha y visibilidad. Si es pública, aparece también en el
   "Repositorio" del socio, solo para ver y descargar. (CU16 flujo)
3. **Given** "Público en Portal Socios" desmarcado, **Then** el acta solo la ve la directiva.
   (CU16 alterno)
4. **Given** un archivo que no es PDF, **Then** se rechaza con "Formato no válido. Solo se
   admiten archivos PDF por seguridad.". Uno mayor a 5 MB se rechaza con "El archivo excede el
   límite permitido". (CU16 excepción)
5. **Given** un acta, **When** el secretario cambia su visibilidad, **Then** queda pública o
   privada según el cambio.
6. **Given** un socio, **Then** no puede borrar ni modificar actas; cualquier intento responde
   403. (CU16 regla)
7. **Given** un ADMIN_JUNTA o TESORERO, **When** abre "Actas", **Then** ve todas las actas y
   puede abrirlas, pero no subir ni cambiar nada. Los intentos responden 403.

---

### User Story 6 - Registrar comunicaciones recibidas (Priority: P2) — CU19

El Secretario/a registra los oficios y notificaciones que llegan de otras instituciones, con un
número de ingreso correlativo y el documento digitalizado.

**Why this priority**: control de correspondencia externa; prioridad media.

**Independent Test**: registrar una comunicación con PDF y otra sin documento, adjuntar el
documento después, eliminar una, y comprobar que el siguiente número no reutiliza el eliminado.

**Acceptance Scenarios**:

1. **Given** un SECRETARIO en "Comunicaciones externas", **When** presiona "Registrar
   Comunicación Recibida", **Then** ve:
   - institución remitente;
   - asunto;
   - fecha de recepción, hoy por defecto;
   - tipo de documento (Oficio, Notificación, Otro);
   - adjunto opcional en PDF, JPG o PNG. (CU19 disparador y flujo, mockup)
2. **Given** datos válidos, **When** presiona "Registrar Comunicación", **Then** se guarda con
   el siguiente número de ingreso de la junta (formato `CE-2026-0001`) y aparece en el listado,
   de la fecha de recepción más reciente a la más antigua. (CU19 flujo)
3. **Given** que no hay documento, **Then** queda marcada "Pendiente de digitalización".
   **When** el secretario adjunta el documento después, **Then** la marca desaparece. (CU19
   alterno)
4. **Given** institución o asunto vacíos, **Then** el botón "Registrar Comunicación" queda
   deshabilitado y los campos se resaltan. Un envío forzado se rechaza con "Este campo es
   obligatorio" en cada campo. (CU19 excepción)
5. **Given** comunicaciones registradas, **When** el secretario elimina una, **Then**:
   - se elimina con su documento;
   - su número no se reutiliza: el siguiente registro toma el número que sigue al último
     asignado;
   - los números no se pueden editar. (CU19 regla)
6. **Given** un archivo de otro tipo o mayor a 5 MB, **Then** se rechaza con "Formato no
   permitido. Use PDF, JPG o PNG" o "El archivo excede el límite permitido".
7. **Given** un ADMIN_JUNTA, **When** abre "Comunicaciones externas", **Then** ve el listado y
   los documentos sin poder registrar, adjuntar ni eliminar. Los intentos responden 403.

---

### User Story 7 - Recibir notificaciones de todos los módulos (Priority: P3) — CU29

Todos los usuarios reciben en la campana las notificaciones de los orígenes que define CU29.
Desde 001 ya funcionan la lista con las no leídas primero, la apertura que marca como leída y
lleva al destino, "Marcar todas como leídas" y el contador. Esta historia agrega los orígenes
que faltaban.

**Why this priority**: es transversal y su base ya existe; aquí se completan los orígenes.

**Independent Test**: provocar cada origen y comprobar que el destinatario correcto recibe la
notificación con el enlace correcto, y que nadie de otra junta la recibe.

**Acceptance Scenarios**:

1. **Given** una convocatoria publicada, de inmediato o programada, **Then** cada socio ACTIVO
   con cuenta activa de la junta recibe "Nueva convocatoria" con el título y la fecha del
   evento, y enlace a "Anuncios". (CU29 origen CU15)
2. **Given** un mensaje nuevo, **Then** cada destinatario recibe "Nuevo mensaje" con el nombre
   del remitente y enlace a esa conversación. Si el mensaje es para "Directiva", lo reciben
   todos los miembros activos de la directiva. (CU29 origen CU18)
3. **Given** una multa nueva (CU10) o una cuota nueva (CU30) para un socio con cuenta activa,
   **Then** el socio recibe "Nueva multa" o "Nueva cuota" con concepto y monto, y enlace a su
   "Estado de Cuenta". (CU29 origen CU10, CU30)
4. **Given** turnos de su sector creados o reprogramados (CU08), solicitudes de recuperación
   (CU21) y correcciones de asistencia (CU28), **Then** siguen notificándose como ya se
   implementó en 001 y 003. Para el socio, el enlace del turno lleva a su "Calendario".
   (CU29 origen CU08, CU21, CU28)
5. **Given** la campana, **Then** su contador muestra solo las no leídas. Se actualiza al
   cargar cada página, y también cada 15 segundos junto con el de mensajes. (CU29 regla)

---

### Edge Cases

- **Socio inactivo o con cuenta desactivada**: no puede iniciar sesión ni recibe notificaciones;
  los mensajes que ya envió se conservan.
- **Miembro de la directiva desactivado**: sus mensajes se conservan; deja de aparecer como
  contacto y de recibir los mensajes nuevos de "Directiva".
- **Borrador con la fecha de publicación ya pasada** (por ejemplo, la aplicación estuvo
  apagada): se publica en la siguiente revisión, siempre que el evento siga siendo futuro. Si
  el evento ya pasó, queda en BORRADOR y se marca "Vencida sin publicar".
- **Respuestas simultáneas**: si dos miembros de la directiva responden a la vez a la misma
  conversación, se guardan ambas respuestas en orden de llegada.
- **Acta eliminada**: las actas no se eliminan desde la interfaz; solo cambian de visibilidad.
- **Número de ingreso**: no comparte numeración entre juntas ni entre años. Cada año empieza en
  0001 y nunca reutiliza un número de ese año.
- **Imagen de un mensaje**: solo la ven los participantes de la conversación. Cualquier otro
  usuario recibe 404.
- **Aislamiento entre juntas**: convocatorias, actas, comunicaciones y mensajes de otra junta
  responden como no encontrados.

## Requirements *(mandatory)*

### Functional Requirements

**Mi información (CU17)**

- **FR-001** (CU17): Al iniciar sesión, el socio MUST llegar a "Mi Panel". Tiene las pestañas
  Resumen, Estado de Cuenta, Mi Predio, Calendario, Anuncios, Repositorio y Mensajes, y solo
  muestra datos del propio socio.
- **FR-002** (CU17): "Estado de Cuenta" MUST mostrar:
  - la deuda total (multas y cuotas no anuladas), en verde si es cero y en rojo si es mayor;
  - el detalle de obligaciones pendientes;
  - los últimos 10 pagos con su estado de reverso;
  - una versión imprimible.
- **FR-003** (CU17): "Mi Predio" MUST mostrar los predios del socio en el mapa. "Calendario"
  MUST mostrar los turnos de su sector de las próximas 4 semanas.
- **FR-004** (CU17): Todo lo que ve el socio MUST ser de solo lectura.

**Convocatorias (CU15)**

- **FR-005** (CU15): El SECRETARIO MUST poder crear convocatorias con título, tipo (ASAMBLEA,
  MINGA), fecha y hora del evento futuras, lugar, orden del día y prioridad (URGENTE, NORMAL).
- **FR-006** (CU15): Una convocatoria URGENTE MUST publicarse al guardarla.
- **FR-007** (CU15): Una convocatoria NORMAL MUST guardarse como BORRADOR con una fecha de
  publicación futura y anterior al evento, y publicarse sola cuando llegue esa fecha. La
  revisión se hace al menos una vez por minuto.
- **FR-008** (CU15): Al publicarse una convocatoria, el sistema MUST notificar a los socios
  ACTIVOS con cuenta activa de la junta.
- **FR-009** (CU15): Las convocatorias a Asamblea MUST mostrarse fijadas arriba para el socio
  hasta la fecha del evento.
- **FR-010** (CU15): Los borradores MUST poder editarse y eliminarse; las publicadas MUST NOT.
- **FR-011** (CU26, CU15): Al guardar una Asamblea o Minga en Eventos, el sistema MUST ofrecer
  crear la convocatoria con los datos del evento precargados.

**Anuncios y repositorio (CU32)**

- **FR-012** (CU32): "Anuncios" MUST listar las convocatorias PUBLICADAS de la junta del
  socio, con las Asambleas cuyo evento no ha pasado primero. "Repositorio" MUST listar sus
  actas públicas y permitir abrirlas o descargarlas.
- **FR-013** (CU32): Sin contenido, cada sección MUST mostrar "No hay publicaciones por
  ahora". Los borradores y las actas privadas MUST NOT ser visibles ni descargables por
  socios.

**Mensajes (CU18)**

- **FR-014** (CU18): Un socio MUST poder escribir solo a "Directiva", una conversación propia
  que comparten todos los miembros activos de la directiva de su junta.
- **FR-015** (CU18): Los miembros de la directiva MUST poder responder en la conversación de
  cualquier socio de su junta, iniciar una con un socio con cuenta activa y conversar entre
  ellos.
- **FR-016** (CU18): Un mensaje MUST tener texto o imagen. El botón Enviar MUST deshabilitarse
  si no hay ninguno, y el servidor MUST validarlo igualmente. La imagen MUST ser JPG o PNG de
  hasta 5 MB y verse como miniatura.
- **FR-017** (CU18): El sistema MUST rechazar mensajes de un socio a otro socio y entre juntas
  distintas.
- **FR-018** (CU18): Los mensajes recibidos MUST marcarse como leídos al abrir la
  conversación. En la conversación "Directiva", un mensaje del socio cuenta como leído cuando
  lo abre cualquier miembro de la directiva.
- **FR-019** (CU18, CU29): Los contadores de mensajes y de la campana MUST actualizarse al
  cargar cada página y cada 15 segundos, sin conexión permanente con el servidor.

**Actas (CU16)**

- **FR-020** (CU16): El SECRETARIO MUST poder subir actas con nombre, fecha de reunión, tipo
  de sesión, descripción, archivo PDF de hasta 5 MB y visibilidad. Es "Público en Portal
  Socios" por defecto.
- **FR-021** (CU16): Los archivos que no sean PDF MUST rechazarse con "Formato no válido. Solo
  se admiten archivos PDF por seguridad.".
- **FR-022** (CU16): El SECRETARIO MUST poder cambiar la visibilidad de un acta. Nadie MUST
  poder eliminarla desde la interfaz, y los socios no pueden modificarla.

**Comunicaciones externas (CU19)**

- **FR-023** (CU19): El SECRETARIO MUST poder registrar comunicaciones con institución
  remitente, asunto, fecha de recepción no futura, tipo (OFICIO, NOTIFICACION, OTRO) y
  documento opcional (PDF, JPG o PNG de hasta 5 MB).
- **FR-024** (CU19): Cada comunicación MUST recibir un número de ingreso correlativo, único
  por junta y año, que no se edita ni se reutiliza aunque se elimine el registro.
- **FR-025** (CU19): Sin documento, la comunicación MUST quedar "Pendiente de digitalización"
  hasta que se adjunte uno.
- **FR-026** (CU19): El botón de registrar MUST deshabilitarse sin institución o asunto, y el
  servidor MUST validarlo igualmente.
- **FR-027** (CU19): El SECRETARIO MUST poder eliminar una comunicación.

**Notificaciones (CU29)**

- **FR-028** (CU29): El sistema MUST notificar:
  - una convocatoria publicada a los socios con cuenta;
  - un mensaje nuevo a sus destinatarios;
  - una multa o cuota nueva al socio con cuenta.

  Cada notificación lleva enlace a su destino. Los orígenes de 001 y 003 se mantienen.

**Transversales**

- **FR-029**: Los permisos MUST seguir la matriz vigente:
  - Convocatorias: SECRETARIO acceso completo; ADMIN_JUNTA solo lectura.
  - Actas: SECRETARIO acceso completo; ADMIN_JUNTA y TESORERO solo lectura.
  - Comunicaciones externas: SECRETARIO acceso completo; ADMIN_JUNTA solo lectura.
  - Mi información, Anuncios y Repositorio: solo el SOCIO.
  - Mensajes: ADMIN_JUNTA, SECRETARIO, TESORERO y SOCIO.
- **FR-030**: Todo dato de esta funcionalidad MUST pertenecer a una junta y solo verse o
  modificarse desde esa junta. Los identificadores de otra junta responden como no
  encontrados.
- **FR-031** (CU25): La tarjeta "Mensajes no leídos" del panel MUST mostrar el número real.
- **FR-032**: Todos los textos MUST estar en español y las pantallas MUST usarse en celular sin
  desplazamiento horizontal de la página.

### Key Entities *(include if feature involves data)*

- **Convocatoria** (nueva): título, tipo (ASAMBLEA, MINGA), fecha y hora del evento, lugar,
  orden del día, prioridad (NORMAL, URGENTE), fecha de publicación, estado (BORRADOR,
  PUBLICADA) y autor. Pertenece a una junta.
- **Acta** (nueva): título, fecha de reunión, tipo de sesión, descripción, documento PDF y
  "pública para socios". Pertenece a una junta.
- **ComunicacionExterna** (nueva): número de ingreso, remitente, asunto, fecha de recepción,
  tipo de documento (OFICIO, NOTIFICACION, OTRO), documento y pendiente de digitalización.
  Pertenece a una junta.
- **Mensaje** (nuevo): contenido, imagen, fecha de envío, remitente, conversación y leído.
- **Conversación** (nueva): agrupa los mensajes de un socio con la directiva, o de dos
  miembros de la directiva entre sí, dentro de una junta. Guarda hasta qué mensaje leyó cada
  participante.
- **Notificacion** (existente, 001): se generan nuevas desde convocatorias, mensajes, multas y
  cuotas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El socio ve su deuda total y sus últimos pagos en menos de 5 segundos desde que
  inicia sesión.
- **SC-002**: El secretario redacta y publica una convocatoria en menos de 2 minutos, y el 100%
  de los socios con cuenta activa la tienen en su campana al publicarse.
- **SC-003**: Una convocatoria programada se publica a más tardar 1 minuto después de su fecha
  de publicación.
- **SC-004**: Un mensaje enviado aparece en el contador del destinatario en menos de 20
  segundos sin que recargue la página.
- **SC-005**: El 100% de los intentos de un socio de escribir a otro socio, ver actas privadas o
  ver datos de otro socio son rechazados.
- **SC-006**: Los números de ingreso de comunicaciones de una junta no tienen repeticiones en el
  año, incluso después de eliminar registros.
- **SC-007**: Todas las pantallas del módulo se usan sin desplazamiento horizontal en un celular
  de 360 px.

## Assumptions

- **Contactos del socio**: la conversación "Directiva" es una por socio, compartida por el
  Administrador, el Secretario/a y el Tesorero/a activos. CU18 nombra al administrador y al
  socio como actores, pero la matriz de seguridad da acceso a Mensajes a los tres roles de la
  directiva.
- **Mensajes**: no se editan ni se eliminan.
- **Avisos externos**: no se envían SMS ni correos. Las casillas "Notificar por SMS" del mockup
  quedan fuera; la notificación es la campana.
- **Editor de la convocatoria**: el orden del día es texto con saltos de línea. Las
  herramientas de formato del mockup (negrita, listas, adjuntos) quedan fuera para mantener la
  simplicidad.
- **Mensaje de error de fecha**: la fecha del evento se pide futura, como dice CU15. La fecha
  de publicación programada también es futura y anterior al evento; el mensaje "La fecha debe
  ser futura" de CU15 se aplica a la del evento.
- **Convocatorias y eventos**: publicar una convocatoria no crea el evento. El evento se crea
  en CU26 y desde ahí se ofrece la convocatoria.
- **Actas**: solo se aceptan en PDF, como dice CU16; el mockup menciona otros formatos solo
  para la vista previa. "Tipo de sesión" usa los tipos de evento (Asamblea, Minga, Reunión).
  No se eliminan: el secretario las oculta quitándoles la visibilidad para socios.
- **Número de ingreso**: `CE-AAAA-NNNN`, correlativo por junta y año. Se calcula a partir del
  mayor número asignado, que queda registrado aunque se elimine la comunicación.
- **Calendario del socio**: muestra 4 semanas desde hoy. "Mi Predio" usa el mismo mapa de 003.
- **Notificaciones de multas y cuotas**: llegan solo a socios con cuenta activa. Una emisión
  masiva de cuotas genera una notificación por socio. Con esto cambia el supuesto de 004 que
  decía que no se notificaba a socios.
- **Recibo del socio**: el socio no reimprime recibos de pago; lo hace el tesorero (CU09). El
  socio imprime su estado de cuenta.
