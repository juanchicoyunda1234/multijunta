# Feature Specification: Módulo Gestión de Plataforma

**Feature Branch**: `002-gestion-plataforma`

**Created**: 2026-11-22

**Status**: Draft

**Input**: User description: "Módulo Gestión de Plataforma: casos de uso CU01 a CU04 y CU23 a CU25 de docs/modulo-1-plataforma.md, con el modelo de docs/modelo-dominio.md. Cada caso de uso es una historia de usuario; su flujo normal, alternos, excepciones y reglas son los criterios de aceptación. Mockups en docs/mockups/CU01 a CU04. Reutiliza lo ya construido en la feature 001 (sesión, roles, matriz de permisos, notificaciones) sin duplicarlo."

**Casos de uso cubiertos**: CU01, CU02, CU03, CU04, CU23, CU24, CU25.

**Depende de la funcionalidad 001** (`specs/001-base-acceso-sesion`), que ya entrega y esta
funcionalidad reutiliza sin redefinir: inicio y cierre de sesión con bloqueo (CU20),
recuperación (CU21) y cambio de contraseña obligatorio (CU22), la matriz de permisos por rol,
el menú por rol, el aislamiento por junta, las páginas 403/404 y la campana de notificaciones.
Toda cuenta creada aquí nace con la marca "debe cambiar contraseña", que la funcionalidad 001
ya hace cumplir.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar nuevas juntas (Priority: P1) — CU01

El Super Administrador ve el listado de juntas de la plataforma y registra una nueva junta con
sus datos básicos desde una ventana emergente.

**Why this priority**: sin juntas no existe ningún otro dato en la plataforma; es la puerta de
entrada de cada organización.

**Independent Test**: como SUPER_ADMIN, abrir el listado, registrar una junta válida, intentar
una con RUC repetido y cancelar un registro.

**Acceptance Scenarios**:

1. **Given** el SUPER_ADMIN con sesión iniciada, **When** entra a su pantalla de inicio,
   **Then** ve el listado de juntas con nombre, RUC, tipo, provincia, cantón, estado (Activa o
   Suspendida) y número de administradores activos, y el botón "+ Nueva Organización".
2. **Given** el listado, **When** presiona "+ Nueva Organización", **Then** se abre la ventana
   "Registro de Nueva Junta" con Nombre, RUC, Tipo, Provincia y Cantón (estos tres como
   selectores; el de Cantón muestra solo los cantones de la provincia elegida).
3. **Given** datos válidos, **When** presiona "Guardar Organización", **Then** el sistema
   guarda la junta como activa con la fecha de registro de hoy, cierra la ventana, actualiza el
   listado y muestra la alerta verde "Junta registrada con éxito".
4. **Given** la ventana abierta, **When** presiona "Cancelar", **Then** se cierra sin guardar
   nada. (CU01 alterno)
5. **Given** un RUC que ya pertenece a otra junta, **When** guarda, **Then** la ventana sigue
   abierta con los datos escritos, el campo RUC se marca en rojo y aparece "Este RUC ya está
   registrado". (CU01 excepción)
6. **Given** un RUC que no tiene exactamente 13 dígitos o un campo obligatorio vacío, **When**
   guarda, **Then** la ventana sigue abierta y el campo se marca con su mensaje ("El RUC debe
   tener 13 dígitos", "Este campo es obligatorio"). (CU01 reglas)

---

### User Story 2 - Asignar Administrador de la Junta (Priority: P1) — CU02

El Super Administrador crea la cuenta del Administrador de una junta desde la fila de esa
junta, y puede reasignar el rol a otra persona.

**Why this priority**: sin administrador la junta no puede configurarse ni crear las demás
cuentas.

**Independent Test**: asignar un administrador a una junta nueva, iniciar sesión con las
credenciales mostradas, reasignar el rol y comprobar que la cuenta anterior quedó desactivada.

**Acceptance Scenarios**:

1. **Given** el listado de juntas, **When** el SUPER_ADMIN presiona el ícono de llave
   "Asignar Administrador" de una junta, **Then** se abre un panel lateral con "Junta destino:
   nombre (RUC)" y un formulario con Cédula, Nombres, Apellidos y Correo electrónico. (CU02
   flujo)
2. **Given** datos válidos y menos de 2 administradores activos en la junta, **When** presiona
   "Generar Cuenta y Asignar Rol", **Then** el sistema crea la cuenta ADMIN_JUNTA en esa junta
   con la cédula como usuario, genera una contraseña aleatoria, marca "debe cambiar contraseña"
   y muestra un resumen con junta, nombre, usuario y contraseña, una sola vez. (CU02 flujo)
3. **Given** una junta con 2 administradores activos, **When** intenta asignar otro sin
   reasignar, **Then** el sistema lo rechaza con "La junta ya tiene 2 administradores activos;
   use Reasignar". (CU02 regla)
4. **Given** una junta con al menos un administrador activo, **When** el SUPER_ADMIN elige
   "Reasignar", selecciona al administrador que se reemplaza y completa los datos de la nueva
   persona, **Then** el sistema desactiva la cuenta elegida antes de crear la nueva y muestra el
   resumen de credenciales. (CU02 alterno)
5. **Given** un correo que ya usa otra cuenta administrativa o de la directiva, **When** genera
   la cuenta, **Then** el sistema la rechaza con "Este correo ya está en uso por otra cuenta
   administrativa" y no crea nada. (CU02 excepción)
6. **Given** una cédula ya registrada en la plataforma (con o sin cuenta), **When** genera la cuenta,
   **Then** el sistema la rechaza con "Esta cédula ya está registrada en la plataforma".
7. **Given** la cuenta recién creada, **When** esa persona inicia sesión con las credenciales
   del resumen, **Then** es llevada a "Cambiar contraseña" antes de poder usar el sistema.
   (CU02 regla, reutiliza CU22)

---

### User Story 3 - Configurar datos generales y sectores (Priority: P1) — CU03

El Administrador de la Junta completa los datos de su junta en tres pestañas (Información
General, Ubicación y Sectores) y los guarda con un solo botón.

**Why this priority**: los sectores son obligatorios para registrar socios y alimentan los
selectores de Secretaría y Tesorería; el monto de multa alimenta CU10.

**Independent Test**: como ADMIN_JUNTA, editar teléfono, logo y monto de multa, completar la
dirección, agregar y quitar sectores, guardar y verificar que la otra junta no cambió.

**Acceptance Scenarios**:

1. **Given** un ADMIN_JUNTA, **When** abre "Configuración de Junta" en el menú lateral,
   **Then** ve las pestañas "Información General" (nombre, RUC y tipo solo lectura; teléfono,
   logo y monto de multa por inasistencia editables), "Ubicación" (provincia y cantón solo
   lectura; dirección editable) y "Sectores" (campo de texto, botón "+ Añadir" y lista de
   sectores con botón [X]). (CU03 flujo)
2. **Given** cambios en varias pestañas, **When** el administrador se mueve entre pestañas,
   **Then** lo escrito no se pierde hasta que guarde o salga de la pantalla. (CU03 alterno)
3. **Given** datos válidos, **When** presiona "Actualizar Datos", **Then** el sistema guarda
   todo (incluidos los sectores añadidos y quitados) y muestra "Datos de la junta actualizados".
   (CU03 flujo)
4. **Given** la Dirección vacía, **When** presiona "Actualizar Datos", **Then** el sistema
   muestra la pestaña "Ubicación", lleva el foco al campo y muestra "La dirección es
   obligatoria"; no guarda nada. (CU03 excepción)
5. **Given** la pestaña Sectores, **When** escribe un nombre y presiona "+ Añadir", **Then** el
   sector aparece en la lista; si el nombre está vacío o ya existe en la junta (sin distinguir
   mayúsculas), se muestra "Ese sector ya existe" o "Escriba el nombre del sector" y no se
   añade.
6. **Given** un sector que tiene socios asignados, **When** el administrador intenta quitarlo,
   **Then** el sistema no lo permite y muestra "No se puede quitar un sector con socios
   asignados".
7. **Given** un logo que no es imagen PNG o JPG, o pesa más de 5 MB, **When** guarda,
   **Then** el sistema lo rechaza con "El logo debe ser una imagen PNG o JPG de hasta 5 MB".
8. **Given** un monto de multa negativo o con más de 2 decimales, **When** guarda, **Then** se
   rechaza con "Ingrese un monto válido (0 o mayor, hasta 2 decimales)".
9. **Given** sectores guardados, **When** otros módulos de la misma junta muestran un selector
   de sector, **Then** listan exactamente esos sectores. (CU03 regla)

---

### User Story 4 - Generar accesos de socios (Priority: P2) — CU04

El Administrador de la Junta crea en bloque las cuentas de los socios que ya están en la nómina
y aún no tienen acceso, y puede desactivar o reactivar cuentas.

**Why this priority**: permite a los socios usar el sistema, pero la directiva puede operar sin
ello.

**Independent Test**: seleccionar varios socios sin cuenta, generar credenciales, iniciar
sesión con una de ellas, desactivar y reactivar una cuenta.

**Acceptance Scenarios**:

1. **Given** un ADMIN_JUNTA, **When** abre la pestaña "Socios" del módulo Usuarios, **Then** ve
   la nómina de socios de su junta con nombre, cédula, sector, estado en la nómina, una casilla
   por socio y el badge "Estado de cuenta" (Sin cuenta, Activa en verde, Desactivada), y el
   contador "X de Y socios seleccionados". (CU04 flujo)
2. **Given** socios sin cuenta seleccionados, **When** presiona "Crear Credenciales (X
   seleccionados)", **Then** el sistema crea para cada uno una cuenta con su cédula como
   usuario, una contraseña temporal distinta por socio y la marca "debe cambiar contraseña";
   el estado pasa a "Activa" y se muestra una sola vez una lista imprimible con nombre, usuario
   y contraseña temporal de cada socio. (CU04 flujo)
3. **Given** la lista, **When** marca la casilla de cabecera, **Then** se seleccionan todos los
   socios sin cuenta que se pueden seleccionar. (CU04 alterno)
4. **Given** un socio INACTIVO en la nómina, **Then** su casilla aparece bloqueada y, aunque se
   envíe su identificador, el sistema no le crea cuenta. (CU04 excepción)
5. **Given** ningún socio seleccionado, **Then** el botón de crear credenciales está
   deshabilitado y, si se envía igual, el sistema muestra "Seleccione al menos un socio".
6. **Given** un socio con cuenta activa, **When** el administrador presiona "Desactivar",
   **Then** la cuenta queda desactivada, ese socio ya no puede iniciar sesión y, si tenía sesión
   abierta, se cierra en su siguiente acción (reutiliza CU20). (CU04 regla)
7. **Given** una cuenta de socio desactivada, **When** presiona "Reactivar", **Then** vuelve a
   estar activa con su contraseña anterior.
8. **Given** el socio con cuenta recién creada, **When** inicia sesión con la temporal,
   **Then** debe cambiarla antes de continuar. (CU04 regla, reutiliza CU22)

---

### User Story 5 - Gestionar cuentas de la directiva (Priority: P2) — CU23

El Administrador de la Junta crea, edita, cambia de rol, desactiva y reactiva las cuentas de
Secretario/a y Tesorero/a de su junta.

**Why this priority**: la directiva necesita cuentas para operar los módulos administrativo y
financiero.

**Independent Test**: crear un secretario, iniciar sesión con la contraseña temporal, cambiarle
el rol a tesorero, desactivarlo y reactivarlo; intentar duplicados.

**Acceptance Scenarios**:

1. **Given** un ADMIN_JUNTA, **When** abre la pestaña "Directiva" del módulo Usuarios,
   **Then** ve las cuentas de la directiva de su junta (administradores, secretarios y
   tesoreros) con nombre, cédula, correo, rol y estado. (CU23 flujo)
2. **Given** "Nueva cuenta", **When** ingresa cédula, nombres, apellidos, correo y rol
   (SECRETARIO o TESORERO) válidos, **Then** el sistema crea la cuenta con la cédula como
   usuario, muestra una sola vez la contraseña temporal y marca "debe cambiar contraseña".
   (CU23 flujo)
3. **Given** una cuenta de la directiva, **When** edita nombres, apellidos, correo o rol,
   **Then** se guardan los cambios; el nuevo rol rige desde la siguiente acción de esa persona.
   (CU23 alterno)
4. **Given** una cuenta activa o desactivada, **When** presiona "Desactivar" o "Reactivar",
   **Then** cambia su estado; una cuenta desactivada no inicia sesión. (CU23 alterno y regla)
5. **Given** una cédula o un correo que ya usa otra cuenta, **When** guarda, **Then** se
   rechaza con "Esta cédula ya está registrada en la plataforma" o "Este correo ya está en uso
   por otra cuenta administrativa". (CU23 excepción)
6. **Given** el formulario, **Then** el selector de rol solo ofrece SECRETARIO y TESORERO; si se
   envía otro rol, el sistema lo rechaza con 403. Las cuentas ADMIN_JUNTA aparecen en la lista
   pero no se pueden editar ni desactivar desde aquí (eso es CU02). (CU23 regla)
7. **Given** cualquier operación, **Then** el administrador solo ve y modifica cuentas de su
   propia junta; un identificador de otra junta responde como no encontrado.

---

### User Story 6 - Editar o suspender juntas (Priority: P2) — CU24

El Super Administrador corrige los datos de una junta, la suspende con un motivo o la
reactiva.

**Why this priority**: necesaria para la administración de la plataforma, pero no bloquea la
operación diaria de las juntas.

**Independent Test**: editar una junta, suspenderla y comprobar que sus usuarios no entran,
reactivarla y comprobar que vuelven a entrar con sus datos intactos.

**Acceptance Scenarios**:

1. **Given** el listado, **When** el SUPER_ADMIN presiona el ícono de lápiz de una junta,
   **Then** se abre la misma ventana de CU01 con los datos cargados; al guardar se actualiza la
   fila y se muestra "Junta actualizada con éxito". (CU24 flujo)
2. **Given** un RUC editado que pertenece a otra junta, **When** guarda, **Then** se muestra
   "Este RUC ya está registrado" como en CU01. (CU24 excepción)
3. **Given** una junta activa, **When** presiona "Suspender", **Then** se pide confirmación y un
   motivo obligatorio; al confirmar la junta queda suspendida con su motivo visible en el
   listado. (CU24 flujo)
4. **Given** una junta suspendida, **Then** sus usuarios no pueden iniciar sesión, las sesiones
   abiertas se cierran en su siguiente acción con "La organización se encuentra suspendida" y
   todos sus datos se conservan. (CU24 regla, reutiliza CU20)
5. **Given** una junta suspendida, **When** presiona "Reactivar" y confirma, **Then** vuelve a
   estar activa, se borra el motivo y sus usuarios activos pueden volver a entrar. (CU24
   alterno)
6. **Given** cualquier junta, **Then** no existe ninguna opción para eliminarla. (CU24 regla)

---

### User Story 7 - Panel de inicio de la directiva (Priority: P3) — CU25

Al iniciar sesión, el Administrador de la Junta, el Secretario/a y el Tesorero/a ven tarjetas
de resumen de su junta según lo que su rol puede consultar.

**Why this priority**: mejora la orientación diaria, pero cada módulo es accesible desde el
menú sin el panel.

**Independent Test**: entrar con cada rol de la directiva y comprobar qué tarjetas ve, sus
valores con los datos de prueba y a dónde lleva cada una.

**Acceptance Scenarios**:

1. **Given** un miembro de la directiva, **When** inicia sesión, **Then** llega al panel con
   tarjetas solo de los módulos a los que su rol tiene acceso según la matriz de seguridad:
   socios activos (los tres roles), próximos eventos y turnos (ADMIN_JUNTA, SECRETARIO),
   asistencias pendientes de cerrar (ADMIN_JUNTA, SECRETARIO, TESORERO), saldo actual y total de
   deuda por cobrar (ADMIN_JUNTA, TESORERO), ítems con stock bajo (ADMIN_JUNTA, TESORERO),
   mensajes no leídos (los tres roles) y solicitudes de recuperación pendientes (solo
   ADMIN_JUNTA). (CU25 flujo y regla)
2. **Given** las tarjetas con datos disponibles, **Then** "Socios activos" muestra el número de
   socios en estado ACTIVO de su junta y "Solicitudes de recuperación pendientes" el número que
   ese administrador puede atender; ambos coinciden con los datos de prueba.
3. **Given** una tarjeta cuyo módulo aún no está construido, **Then** se muestra con el texto
   "Disponible próximamente" en lugar de un número.
4. **Given** cualquier tarjeta, **When** el usuario la presiona, **Then** lo lleva al módulo
   correspondiente (o a "Módulo en construcción" si aún no existe). (CU25 regla)
5. **Given** dos juntas, **Then** cada panel muestra solo los números de la junta del usuario.

---

### Edge Cases

- Provincia cambiada en la ventana de CU01: el selector de cantón se vacía y muestra solo los
  cantones de la nueva provincia; un cantón que no corresponde a la provincia se rechaza.
- RUC con espacios o guiones: se rechaza; solo se aceptan 13 dígitos.
- Asignar administrador a una junta suspendida: se permite (para preparar su reactivación),
  pero esa persona no entra hasta que la junta se reactive.
- Reasignar cuando el administrador reemplazado tiene sesión abierta: su sesión se cierra en la
  siguiente acción (reutiliza CU20).
- Cambio de rol en CU23 de una persona con sesión abierta: el menú y los permisos nuevos rigen
  desde su siguiente acción.
- Socio sin cuenta que es reactivado: no aplica; "Reactivar" solo existe para cuentas
  desactivadas.
- Dos administradores guardan a la vez la configuración de la misma junta: gana el último en
  guardar (sin bloqueo).
- Junta sin sectores: el administrador ve en la pestaña Sectores el aviso "Registre al menos un
  sector para poder inscribir socios".
- El administrador intenta desactivar su propia cuenta en CU23: no es posible (las cuentas
  ADMIN_JUNTA no se gestionan aquí).

## Requirements *(mandatory)*

### Functional Requirements

**Listado y registro de juntas (CU01)**

- **FR-001** (CU01): La pantalla de inicio del SUPER_ADMIN MUST ser el listado de juntas con
  nombre, RUC, tipo, provincia, cantón, estado, motivo de suspensión (si aplica) y número de
  administradores activos.
- **FR-002** (CU01): El SUPER_ADMIN MUST poder registrar una junta con nombre, RUC, tipo,
  provincia y cantón en una ventana emergente, con "Guardar Organización" y "Cancelar".
- **FR-003** (CU01): El RUC MUST tener exactamente 13 dígitos y ser único en la plataforma; el
  error de unicidad se muestra en el campo con "Este RUC ya está registrado" sin cerrar la
  ventana ni perder lo escrito.
- **FR-004** (CU01): Provincia y cantón MUST elegirse de un catálogo de provincias y cantones
  del Ecuador; el cantón MUST pertenecer a la provincia elegida.
- **FR-005** (CU01): Al guardar, la junta MUST quedar activa, con fecha de registro de hoy e
  identificador generado por el sistema, y se MUST mostrar "Junta registrada con éxito".

**Administrador de la junta (CU02)**

- **FR-006** (CU02): Desde cada fila, el SUPER_ADMIN MUST poder abrir un panel lateral para
  asignar un administrador con cédula, nombres, apellidos y correo.
- **FR-007** (CU02): La cuenta creada MUST ser ADMIN_JUNTA de esa junta, usar la cédula como
  usuario, tener una contraseña aleatoria mostrada una sola vez y la marca "debe cambiar
  contraseña".
- **FR-008** (CU02): Una junta MUST NOT tener más de 2 cuentas ADMIN_JUNTA activas.
- **FR-009** (CU02): "Reasignar" MUST desactivar la cuenta administrativa elegida antes de
  crear la nueva, en una sola operación.
- **FR-010** (CU02, CU23): El correo de una cuenta ADMIN_JUNTA, SECRETARIO o TESORERO MUST ser
  único entre todas esas cuentas de la plataforma.
- **FR-011** (CU02, CU04, CU23): La cédula MUST seguir siendo única en toda la plataforma
  (regla de la funcionalidad 001), incluso frente a socios que aún no tienen cuenta; un intento
  duplicado se rechaza con "Esta cédula ya está registrada en la plataforma".

**Configuración de la junta (CU03)**

- **FR-012** (CU03): El ADMIN_JUNTA MUST poder editar, solo para su junta, teléfono, logo,
  monto de multa por inasistencia y dirección; nombre, RUC, tipo, provincia y cantón se muestran
  solo lectura.
- **FR-013** (CU03): La pantalla MUST organizarse en las pestañas "Información General",
  "Ubicación" y "Sectores", conservar lo escrito al cambiar de pestaña y guardar todo con
  "Actualizar Datos".
- **FR-014** (CU03): La dirección MUST ser obligatoria al guardar; si falta, se MUST mostrar la
  pestaña Ubicación con el foco en el campo y el mensaje "La dirección es obligatoria".
- **FR-015** (CU03): El logo MUST ser PNG o JPG de hasta 5 MB y MUST mostrarse junto al nombre
  de la junta en la barra superior de sus usuarios.
- **FR-016** (CU03): El monto de multa por inasistencia MUST ser 0 o mayor con hasta 2
  decimales.
- **FR-017** (CU03): El ADMIN_JUNTA MUST poder añadir sectores (nombre obligatorio, único en su
  junta sin distinguir mayúsculas) y quitar sectores sin socios asignados.
- **FR-018** (CU03): Los sectores de la junta MUST ser la única fuente de los selectores de
  sector del resto de módulos de esa junta.

**Accesos de socios (CU04)**

- **FR-019** (CU04): El ADMIN_JUNTA MUST ver la nómina de socios de su junta con estado de
  cuenta (Sin cuenta, Activa, Desactivada) y selección individual y de cabecera.
- **FR-020** (CU04): "Crear Credenciales" MUST crear, para cada socio seleccionado ACTIVO en la
  nómina y sin cuenta, una cuenta con su cédula como usuario, contraseña temporal propia y la
  marca "debe cambiar contraseña", y mostrar una sola vez la lista imprimible de credenciales.
- **FR-021** (CU04): Los socios INACTIVOS en la nómina MUST NOT recibir cuenta, aunque se envíe
  su identificador.
- **FR-022** (CU04): El ADMIN_JUNTA MUST poder desactivar y reactivar cuentas de socios de su
  junta; una cuenta desactivada no inicia sesión.

**Cuentas de la directiva (CU23)**

- **FR-023** (CU23): El ADMIN_JUNTA MUST poder listar las cuentas de la directiva de su junta y
  crear cuentas SECRETARIO o TESORERO con cédula, nombres, apellidos, correo y rol, usando la
  cédula como usuario, con contraseña temporal mostrada una sola vez y la marca "debe cambiar
  contraseña".
- **FR-024** (CU23): El ADMIN_JUNTA MUST poder editar nombres, apellidos, correo y rol
  (entre SECRETARIO y TESORERO), y desactivar o reactivar esas cuentas.
- **FR-025** (CU23): El ADMIN_JUNTA MUST NOT crear, editar, cambiar a, desactivar ni reactivar
  cuentas SUPER_ADMIN o ADMIN_JUNTA; cualquier intento responde 403.
- **FR-026** (CU23): Un cambio de rol o de estado MUST regir desde la siguiente acción de la
  persona afectada.

**Edición y suspensión de juntas (CU24)**

- **FR-027** (CU24): El SUPER_ADMIN MUST poder editar nombre, RUC, tipo, provincia y cantón de
  una junta con las mismas validaciones de CU01 y el mensaje "Junta actualizada con éxito".
- **FR-028** (CU24): El SUPER_ADMIN MUST poder suspender una junta con confirmación y motivo
  obligatorio (hasta 300 caracteres) y reactivarla con confirmación; al reactivar se borra el
  motivo.
- **FR-029** (CU24): La suspensión MUST impedir el ingreso de los usuarios de esa junta y cerrar
  sus sesiones abiertas, conservando todos sus datos (reutiliza CU20).
- **FR-030** (CU24): El sistema MUST NOT ofrecer ninguna forma de eliminar una junta.

**Panel de la directiva (CU25)**

- **FR-031** (CU25): El panel de ADMIN_JUNTA, SECRETARIO y TESORERO MUST mostrar las tarjetas de
  la Historia 7, solo las permitidas a su rol según la matriz de seguridad.
- **FR-032** (CU25): Las tarjetas "Socios activos" y "Solicitudes de recuperación pendientes"
  MUST mostrar números reales de la junta del usuario; las tarjetas de módulos aún no
  construidos MUST mostrar "Disponible próximamente".
- **FR-033** (CU25): Cada tarjeta MUST llevar a su módulo.

**Transversales**

- **FR-034**: Toda operación de esta funcionalidad MUST respetar la matriz de
  `docs/contexto/seguridad.md` y el aislamiento por junta ya aplicados por la funcionalidad 001;
  un registro de otra junta responde como no encontrado.
- **FR-035**: Todas las contraseñas generadas MUST cumplir la regla de CU22 (al menos 8
  caracteres, una letra y un número) y mostrarse una sola vez; recargar la página no las vuelve
  a mostrar.
- **FR-036**: Todos los textos MUST estar en español y todas las pantallas MUST ser usables en
  computador y celular; las tablas largas se desplazan dentro de su contenedor sin desplazar la
  página.

### Key Entities *(include if feature involves data)*

- **Junta** (existente): se usan además tipo, provincia, cantón, dirección, teléfono, logo,
  monto de multa por inasistencia, activa y motivo de suspensión.
- **Usuario** (existente): cuentas ADMIN_JUNTA, SECRETARIO y TESORERO; el correo pasa a ser
  único entre cuentas administrativas y de la directiva.
- **Socio** (existente): se usan estado en la nómina, tiene cuenta y sector.
- **Sector** (existente): nombre único por junta sin distinguir mayúsculas; no se quita si
  tiene socios.
- **Catálogo de provincias y cantones del Ecuador** (nuevo): lista fija de provincias, cada una
  con sus cantones, usada por los selectores de CU01 y CU24.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El SUPER_ADMIN registra una junta y le asigna su administrador en menos de 3
  minutos.
- **SC-002**: Un administrador nuevo configura su junta con dirección y 3 sectores en menos de 5
  minutos.
- **SC-003**: Se generan las credenciales de 50 socios en una sola operación y la lista
  imprimible queda lista en menos de 10 segundos.
- **SC-004**: El 100% de las excepciones de CU01 a CU04, CU23 y CU24 muestran exactamente los
  mensajes especificados.
- **SC-005**: El 100% de los intentos de ver o modificar juntas, cuentas o sectores de otra junta
  son rechazados sin mostrar datos.
- **SC-006**: Ninguna junta llega a tener más de 2 administradores activos.
- **SC-007**: Todas las pantallas de esta funcionalidad se usan sin desplazamiento horizontal de
  la página en un celular de 360 px.

## Assumptions

- Todas las cuentas creadas en CU02, CU04 y CU23 usan la cédula como usuario, así cada persona
  entra siempre con su cédula; las cuentas de los datos de prueba de la funcionalidad 001
  conservan sus usuarios actuales. Por eso "usuario repetido" de CU23 queda cubierto por la
  regla de cédula única.
- El mockup de CU02 muestra "Nombres y apellidos" en un solo campo; se piden en dos campos
  porque el modelo de dominio los guarda por separado.
- El mockup de CU01 no muestra el tipo de junta; se agrega el selector Tipo porque el modelo lo
  exige. Dirección y teléfono se completan en CU03.
- En CU04 la "clave genérica" se interpreta como una contraseña temporal distinta por socio,
  más segura que una única clave común; todas exigen cambio en el primer ingreso.
- La pestaña "Socios" del módulo Usuarios (CU04) y la pestaña "Directiva" (CU23) son dos
  pestañas de la misma pantalla "Usuarios".
- El correo es único solo entre cuentas ADMIN_JUNTA, SECRETARIO y TESORERO; los socios pueden
  compartir correo (por ejemplo, familias) o no tenerlo.
- Mientras no existan los módulos de eventos, asistencia, finanzas, inventario y mensajes, sus
  tarjetas del panel muestran "Disponible próximamente"; cada funcionalidad posterior completa
  su tarjeta.
- El registro de socios en la nómina es CU05; aquí se usan los socios existentes (datos de
  prueba).
- El catálogo de provincias y cantones se carga con la aplicación y no se edita desde la
  interfaz.
- No se envían correos: todas las credenciales se entregan en persona.
- No se agregan notificaciones nuevas en esta funcionalidad; la campana sigue mostrando las de
  recuperación de contraseña (funcionalidad 001).
