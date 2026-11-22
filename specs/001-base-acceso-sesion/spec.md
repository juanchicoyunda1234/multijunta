# Feature Specification: Base del sistema, acceso por roles y gestión de sesión

**Feature Branch**: `001-base-acceso-sesion`

**Created**: 2026-11-22

**Status**: Draft

**Input**: User description: "Base del Sistema Multi-Junta: acceso por roles con aislamiento de datos por junta, menú según rol, campana de notificaciones y datos de prueba, más los casos de uso CU20 (iniciar y cerrar sesión), CU21 (recuperar contraseña) y CU22 (cambiar contraseña) descritos en docs/modulo-1-plataforma.md. Roles y permisos en docs/contexto/seguridad.md. Usa el flujo normal, alternos, excepciones y reglas de cada caso de uso como criterios de aceptación."

**Casos de uso cubiertos**: CU20, CU21, CU22 (completos); CU29 (solo la campana y la
notificación de solicitud de recuperación); base transversal de la matriz de
`docs/contexto/seguridad.md` para todos los CU.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Iniciar y cerrar sesión según el rol (Priority: P1) — CU20

Cualquier usuario de la plataforma (Super Administrador, Administrador de la Junta,
Secretario/a, Tesorero/a o Socio) abre la aplicación, ingresa su usuario y contraseña y llega
a la pantalla de inicio que corresponde a su rol, dentro de los datos de su junta. Al terminar,
cierra sesión desde la barra superior.

**Why this priority**: sin inicio de sesión ningún otro caso de uso es accesible; es la puerta
de entrada de todo el sistema.

**Independent Test**: con los usuarios de prueba, iniciar sesión con cada rol, comprobar el
destino y el mensaje de cada excepción, y cerrar sesión.

**Acceptance Scenarios**:

1. **Given** un usuario sin sesión activa, **When** abre cualquier página de la aplicación,
   **Then** el sistema muestra el formulario de inicio de sesión con usuario, contraseña,
   botón "Iniciar Sesión" y enlace "¿Olvidaste tu contraseña?". (CU20 disparador)
2. **Given** un miembro de la directiva (ADMIN_JUNTA, SECRETARIO o TESORERO) con cuenta
   activa, **When** ingresa credenciales correctas y presiona "Iniciar Sesión", **Then** el
   sistema lo lleva al panel de inicio de la directiva de su junta. (CU20 flujo)
3. **Given** un SOCIO con cuenta activa, **When** ingresa credenciales correctas, **Then** el
   sistema lo lleva a "Mi información". (CU20 flujo)
4. **Given** el SUPER_ADMIN, **When** ingresa credenciales correctas, **Then** el sistema lo
   lleva al listado de juntas. (CU20 flujo)
5. **Given** una cuenta marcada como "debe cambiar contraseña", **When** inicia sesión
   correctamente, **Then** el sistema lo lleva a "Cambiar contraseña" y cualquier otra página
   que intente abrir lo devuelve allí hasta que la cambie. (CU20 alterno)
6. **Given** un usuario sin sesión, **When** ingresa un usuario inexistente o una contraseña
   incorrecta, **Then** el sistema muestra "Usuario o contraseña inválidos" sin indicar cuál
   de los dos falló. (CU20 excepción)
7. **Given** una cuenta desactivada, **When** ingresa credenciales correctas, **Then** el
   sistema no inicia la sesión y muestra "Cuenta desactivada, contacte a la directiva".
   (CU20 excepción)
8. **Given** un usuario cuya junta está suspendida, **When** ingresa credenciales correctas,
   **Then** el sistema no inicia la sesión y muestra "La organización se encuentra
   suspendida". (CU20 excepción)
9. **Given** una cuenta con 4 intentos fallidos seguidos, **When** falla un quinto intento,
   **Then** la cuenta queda bloqueada 15 minutos y la respuesta a ese mismo quinto intento ya
   muestra "Cuenta bloqueada temporalmente, intente de nuevo en 15 minutos"; durante el
   bloqueo, incluso con la contraseña correcta, se muestra el mismo mensaje. (CU20 regla)
10. **Given** una cuenta con intentos fallidos previos, **When** inicia sesión correctamente
    antes de llegar a 5, **Then** el contador de intentos fallidos vuelve a cero. (CU20 regla)
11. **Given** un usuario con sesión activa, **When** presiona "Cerrar Sesión" en la barra
    superior, **Then** la sesión termina, vuelve al formulario de inicio y al usar "atrás" en
    el navegador no puede ver páginas protegidas. (CU20 regla)

---

### User Story 2 - Acceso por rol y aislamiento por junta (Priority: P1) — base de seguridad

Cada usuario solo ve en el menú las opciones que su rol permite según la matriz de
`docs/contexto/seguridad.md`, solo puede abrir las páginas de esas opciones y solo ve datos de
su propia junta.

**Why this priority**: es la garantía de seguridad sobre la que se construyen todos los
módulos; una fuga entre juntas o entre roles invalida el sistema.

**Independent Test**: con los usuarios de prueba de las dos juntas, revisar el menú de cada
rol, intentar abrir por dirección directa páginas de otro rol y datos de la otra junta.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado, **When** ve la pantalla, **Then** el menú lateral muestra
   solo las opciones de los casos de uso en los que su rol tiene "✔" o "ver" en la matriz, y
   la barra superior muestra su nombre, su rol, el nombre de su junta (excepto SUPER_ADMIN),
   la campana, "Cambiar contraseña" y "Cerrar Sesión".
2. **Given** un usuario con un rol sin acceso a una página, **When** escribe su dirección
   directamente, **Then** el sistema responde con una página "Acceso denegado" (403) en
   español con un enlace para volver a su inicio.
3. **Given** un usuario con acceso "ver" a un módulo, **When** intenta crear, editar o
   eliminar en ese módulo (incluso enviando la operación directamente), **Then** el sistema la
   rechaza con la página 403.
4. **Given** un usuario de la junta A, **When** intenta abrir o modificar por dirección o
   identificador un registro de la junta B, **Then** el sistema responde como recurso no
   encontrado y no muestra ningún dato de la junta B.
5. **Given** el SUPER_ADMIN, **When** intenta abrir una página de los módulos internos de una
   junta (por ejemplo, nómina de socios), **Then** el sistema responde con la página 403.
6. **Given** una página que no existe, **When** cualquier usuario la abre, **Then** el sistema
   muestra una página "Página no encontrada" (404) en español.

---

### User Story 3 - Cambiar contraseña (Priority: P1) — CU22

Cualquier usuario cambia su contraseña desde el menú de usuario, o de forma obligatoria tras
su primer ingreso o una recuperación.

**Why this priority**: todas las cuentas nuevas y recuperadas nacen con contraseña temporal;
sin este caso de uso no pueden empezar a usar el sistema.

**Independent Test**: iniciar sesión con un usuario de prueba marcado para cambiar
contraseña, probar cada excepción y luego un cambio válido.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado, **When** abre "Cambiar contraseña", **Then** el sistema
   pide contraseña actual, nueva contraseña y confirmación. (CU22 flujo)
2. **Given** datos válidos, **When** el usuario guarda, **Then** el sistema guarda la nueva
   contraseña, muestra "Contraseña actualizada correctamente", quita la marca "debe cambiar
   contraseña" y desde ese momento solo la nueva contraseña permite iniciar sesión.
   (CU22 flujo)
3. **Given** una contraseña actual incorrecta, **When** guarda, **Then** el sistema muestra
   "La contraseña actual es incorrecta" y no cambia nada. (CU22 excepción)
4. **Given** nueva contraseña y confirmación distintas, **When** guarda, **Then** el sistema
   muestra "La confirmación no coincide con la nueva contraseña". (CU22 excepción)
5. **Given** una nueva contraseña igual a la actual, **When** guarda, **Then** el sistema
   muestra "La nueva contraseña debe ser distinta de la anterior". (CU22 excepción)
6. **Given** una nueva contraseña de menos de 8 caracteres o sin al menos una letra y un
   número, **When** guarda, **Then** el sistema muestra "La contraseña debe tener al menos 8
   caracteres, con al menos una letra y un número". (CU22 regla)
7. **Given** un usuario obligado a cambiar la contraseña, **When** la cambia correctamente,
   **Then** el sistema lo lleva a la pantalla de inicio de su rol. (CU20 alterno + CU22)

---

### User Story 4 - Recuperar contraseña con apoyo de la directiva (Priority: P2) — CU21

Un usuario que olvidó su contraseña la solicita desde la pantalla de inicio de sesión; el
Administrador de su junta (o el Super Administrador, si quien solicita es un Administrador de
la Junta) verifica a la persona, restablece la contraseña y se la entrega en persona.

**Why this priority**: es necesaria para la operación diaria, pero el sistema puede usarse sin
ella mientras las cuentas recuerden su contraseña.

**Independent Test**: solicitar recuperación con un socio de prueba, atenderla con el
administrador de su junta, iniciar sesión con la contraseña temporal y verificar el cambio
obligatorio.

**Acceptance Scenarios**:

1. **Given** la pantalla de inicio de sesión, **When** el usuario presiona "¿Olvidaste tu
   contraseña?", ingresa su cédula o usuario y envía, **Then** el sistema registra una
   solicitud pendiente y muestra "Tu solicitud fue enviada a la directiva". (CU21 flujo)
2. **Given** una solicitud pendiente de un SOCIO, SECRETARIO o TESORERO, **When** un
   ADMIN_JUNTA de la misma junta abre "Solicitudes de recuperación", **Then** la ve en la lista
   con nombre, cédula, rol y fecha de la solicitud; los administradores de otras juntas no la
   ven. (CU21 flujo)
3. **Given** una solicitud pendiente visible, **When** el administrador presiona "Restablecer"
   y confirma, **Then** el sistema genera una contraseña temporal, la muestra una sola vez en
   pantalla, marca la cuenta con "debe cambiar contraseña", desbloquea la cuenta si estaba
   bloqueada por intentos fallidos y marca la solicitud como atendida con la fecha de
   atención. (CU21 flujo)
4. **Given** una solicitud de un ADMIN_JUNTA, **When** el SUPER_ADMIN abre "Solicitudes de
   recuperación", **Then** la ve y puede atenderla de la misma forma; los ADMIN_JUNTA no ven
   solicitudes de otros administradores. (CU21 alterno)
5. **Given** una cédula o usuario que no existe, **When** se envía la solicitud, **Then** el
   sistema muestra el mismo mensaje "Tu solicitud fue enviada a la directiva" y no registra
   ninguna solicitud. (CU21 excepción)
6. **Given** una solicitud ya atendida, **When** alguien intenta restablecerla de nuevo
   (incluso enviando la operación directamente), **Then** el sistema lo rechaza con "La
   solicitud ya fue atendida". (CU21 regla)
7. **Given** un usuario que ya tiene una solicitud pendiente, **When** envía otra, **Then** el
   sistema muestra el mismo mensaje y no crea una solicitud duplicada.
8. **Given** una contraseña temporal ya mostrada, **When** el administrador recarga o vuelve a
   la página, **Then** la contraseña ya no se muestra. (CU21 regla)

---

### User Story 5 - Campana de notificaciones (Priority: P2) — CU29 (base)

Todo usuario ve en la barra superior una campana con el número de notificaciones no leídas;
al abrirla ve la lista, abre una y el sistema la marca como leída y lo lleva a lo que
corresponde. En esta funcionalidad el único origen de notificaciones es la solicitud de
recuperación de contraseña (CU21); los demás orígenes se agregan con sus casos de uso.

**Why this priority**: los administradores necesitan enterarse de las solicitudes de
recuperación, y la campana es la base que reutilizan los demás módulos.

**Independent Test**: crear una solicitud de recuperación con un socio de prueba y comprobar
que el administrador de su junta ve la campana con contador, abre la notificación y llega a la
lista de solicitudes.

**Acceptance Scenarios**:

1. **Given** un usuario con notificaciones no leídas, **When** carga cualquier página, **Then**
   la campana muestra el número de no leídas; sin no leídas no muestra número. (CU29 regla)
2. **Given** la campana, **When** el usuario la abre, **Then** ve su lista de notificaciones
   con título, mensaje y fecha, con las no leídas primero y destacadas. (CU29 flujo)
3. **Given** una notificación, **When** el usuario la abre, **Then** el sistema la marca como
   leída y lo lleva a su enlace. (CU29 flujo)
4. **Given** varias no leídas, **When** presiona "Marcar todas como leídas", **Then** todas
   quedan leídas y el contador desaparece. (CU29 alterno)
5. **Given** una nueva solicitud de recuperación de un SOCIO, SECRETARIO o TESORERO, **When**
   se registra, **Then** cada ADMIN_JUNTA activo de esa junta recibe la notificación "Nueva
   solicitud de recuperación de contraseña" con enlace a la lista de solicitudes; si la
   solicitud es de un ADMIN_JUNTA, la recibe el SUPER_ADMIN. (CU29 origen CU21)
6. **Given** un usuario, **When** abre la campana, **Then** solo ve sus propias
   notificaciones, nunca las de otro usuario ni de otra junta.

---

### User Story 6 - Datos de prueba al arrancar (Priority: P1) — soporte de verificación

Al arrancar la aplicación sin datos, el sistema carga dos juntas con un usuario por rol en
cada una, sectores y socios, para poder verificar los casos de uso y el aislamiento entre
juntas.

**Why this priority**: sin estos datos no se pueden ejecutar las pruebas de aceptación de
ninguna historia.

**Independent Test**: arrancar con la base vacía e iniciar sesión con cada usuario
documentado en el README.

**Acceptance Scenarios**:

1. **Given** una base de datos vacía, **When** arranca la aplicación, **Then** existen un
   SUPER_ADMIN y dos juntas activas de tipos distintos, cada una con un ADMIN_JUNTA, un
   SECRETARIO, un TESORERO, al menos dos sectores y al menos tres socios (al menos uno con
   cuenta activa y uno INACTIVO).
2. **Given** datos ya cargados, **When** la aplicación se reinicia, **Then** no se duplican.
3. **Given** el README, **When** se lee, **Then** lista usuario, contraseña, rol y junta de
   cada usuario de prueba.
4. **Given** los datos de prueba, **Then** existe al menos un usuario marcado para cambiar
   contraseña, uno desactivado y una tercera junta suspendida con un usuario, para verificar
   las excepciones de CU20.

---

### Edge Cases

- Usuario ingresado con espacios al inicio o al final: se ignoran los espacios.
- Usuario en mayúsculas o minúsculas distintas: el usuario no distingue mayúsculas; la
  contraseña sí.
- Cuenta bloqueada por intentos que solicita recuperación: al restablecerla se desbloquea.
- Junta suspendida mientras un usuario tiene sesión abierta: en su siguiente acción la sesión
  termina y se muestra "La organización se encuentra suspendida".
- Cuenta desactivada mientras tiene sesión abierta: en su siguiente acción la sesión termina y
  se muestra "Cuenta desactivada, contacte a la directiva".
- Sesión inactiva más de 30 minutos: expira y el usuario vuelve al inicio de sesión con el
  aviso "Tu sesión expiró, inicia sesión de nuevo".
- Solicitud de recuperación del SUPER_ADMIN: se muestra el mensaje genérico y no se crea
  solicitud (no hay quién la atienda dentro de la plataforma).
- Junta sin ADMIN_JUNTA activo cuando un socio solicita recuperación: la solicitud queda
  pendiente y la ve el próximo administrador asignado.
- Usuario obligado a cambiar contraseña que presiona "Cerrar Sesión": puede cerrar sesión; al
  volver a entrar sigue obligado.

## Requirements *(mandatory)*

### Functional Requirements

**Inicio y cierre de sesión (CU20)**

- **FR-001** (CU20): El sistema MUST exigir sesión iniciada para todas las páginas excepto
  inicio de sesión, solicitud de recuperación y recursos públicos de presentación.
- **FR-002** (CU20): El sistema MUST autenticar con usuario y contraseña y, al validar,
  identificar el rol y la junta del usuario.
- **FR-003** (CU20): Tras autenticar, el sistema MUST dirigir a SUPER_ADMIN al listado de
  juntas, a ADMIN_JUNTA, SECRETARIO y TESORERO al panel de la directiva y a SOCIO a "Mi
  información". Mientras esas pantallas no existan, MUST mostrar una pantalla de inicio del
  rol con su nombre, rol y junta.
- **FR-004** (CU20): Si la cuenta tiene la marca "debe cambiar contraseña", el sistema MUST
  llevar al usuario a "Cambiar contraseña" y redirigir allí cualquier otra página, salvo
  "Cerrar Sesión".
- **FR-005** (CU20): Ante usuario inexistente o contraseña incorrecta, el sistema MUST mostrar
  "Usuario o contraseña inválidos".
- **FR-006** (CU20): Con credenciales correctas, el sistema MUST rechazar el ingreso de una
  cuenta desactivada con "Cuenta desactivada, contacte a la directiva" y de una junta
  suspendida con "La organización se encuentra suspendida".
- **FR-007** (CU20): El sistema MUST contar los intentos fallidos seguidos por cuenta; al
  quinto MUST bloquear la cuenta 15 minutos y mostrar "Cuenta bloqueada temporalmente,
  intente de nuevo en 15 minutos" desde la respuesta a ese quinto intento; un ingreso correcto MUST reiniciar el contador.
- **FR-008** (CU20): "Cerrar Sesión" MUST terminar la sesión y volver al inicio de sesión; las
  páginas protegidas MUST NOT quedar visibles al usar "atrás".
- **FR-009** (CU20): La sesión MUST expirar tras 30 minutos de inactividad.
- **FR-010** (CU20): Si la cuenta se desactiva o la junta se suspende con la sesión abierta, la
  sesión MUST terminar en la siguiente acción del usuario con el mensaje correspondiente.

**Acceso por rol y aislamiento (base de seguridad para todos los CU)**

- **FR-011**: El sistema MUST manejar exactamente los roles SUPER_ADMIN, ADMIN_JUNTA,
  SECRETARIO, TESORERO y SOCIO; cada usuario tiene un solo rol.
- **FR-012**: Todo usuario excepto SUPER_ADMIN MUST pertenecer a una sola junta.
- **FR-013**: El sistema MUST aplicar la matriz de `docs/contexto/seguridad.md` en el servidor
  a cada página y operación: "✔" permite todo, "ver" solo lectura, y sin marca se niega.
- **FR-014**: Todo acceso negado por rol MUST responder con una página 403 en español con
  enlace al inicio del rol.
- **FR-015**: Toda consulta y operación de un usuario de junta MUST limitarse a los datos de su
  junta, tomada de su sesión y nunca de datos enviados por el navegador.
- **FR-016**: Un registro de otra junta solicitado por dirección o identificador MUST
  responder como no encontrado, sin revelar datos.
- **FR-017**: SUPER_ADMIN MUST NOT acceder a los módulos internos de las juntas.
- **FR-018**: El menú lateral MUST mostrar solo las opciones de los casos de uso permitidos al
  rol ("✔" o "ver"); las opciones cuyo módulo aún no está construido MUST llevar a una página
  "Módulo en construcción".
- **FR-019**: La barra superior MUST mostrar nombre del usuario, rol, nombre de la junta
  (excepto SUPER_ADMIN), campana, "Cambiar contraseña" y "Cerrar Sesión".
- **FR-020**: Una dirección inexistente MUST mostrar una página 404 en español.
- **FR-021**: Las contraseñas MUST guardarse de forma irreversible (nunca en texto legible).

**Recuperar contraseña (CU21)**

- **FR-022** (CU21): La pantalla de inicio de sesión MUST ofrecer "¿Olvidaste tu contraseña?",
  donde se ingresa cédula o usuario.
- **FR-023** (CU21): El identificador se busca primero como usuario y, si no hay
  coincidencia, como cédula. Si la cuenta existe, está activa, no es SUPER_ADMIN y no tiene ya una
  solicitud pendiente, el sistema MUST registrar una solicitud pendiente con fecha; en todos
  los casos MUST mostrar "Tu solicitud fue enviada a la directiva".
- **FR-024** (CU21): Las solicitudes de SOCIO, SECRETARIO y TESORERO MUST ser visibles y
  atendibles solo por los ADMIN_JUNTA de la misma junta; las de ADMIN_JUNTA, solo por el
  SUPER_ADMIN.
- **FR-025** (CU21): "Restablecer" MUST generar una contraseña temporal aleatoria que cumpla la
  regla de FR-031, mostrarla una sola vez, marcar la cuenta con "debe cambiar contraseña",
  reiniciar intentos fallidos y bloqueo, y marcar la solicitud como atendida con fecha de
  atención.
- **FR-026** (CU21): Una solicitud atendida MUST NOT poder atenderse de nuevo.
- **FR-027** (CU21/CU29): Al registrarse una solicitud, el sistema MUST notificar a quienes
  pueden atenderla.

**Cambiar contraseña (CU22)**

- **FR-028** (CU22): Todo usuario autenticado MUST poder cambiar su contraseña indicando la
  actual, la nueva y la confirmación.
- **FR-029** (CU22): El sistema MUST rechazar el cambio si la actual es incorrecta, si nueva y
  confirmación difieren o si la nueva es igual a la actual, con los mensajes de la Historia 3.
- **FR-030** (CU22): Al cambiar correctamente, el sistema MUST quitar la marca "debe cambiar
  contraseña" y confirmar con "Contraseña actualizada correctamente".
- **FR-031** (CU22): Toda contraseña nueva MUST tener al menos 8 caracteres, al menos una letra
  y al menos un número.

**Campana de notificaciones (CU29 base)**

- **FR-032** (CU29): La barra superior MUST mostrar una campana con el número de
  notificaciones no leídas del usuario, recalculado al cargar cada página.
- **FR-033** (CU29): La lista de notificaciones MUST mostrar título, mensaje y fecha, con las no
  leídas primero.
- **FR-034** (CU29): Abrir una notificación MUST marcarla como leída y llevar a su enlace.
- **FR-035** (CU29): "Marcar todas como leídas" MUST marcar todas las del usuario.
- **FR-036** (CU29): Un usuario MUST ver solo sus propias notificaciones.

**Datos de prueba (soporte)**

- **FR-037**: Con la base vacía, el sistema MUST cargar los datos de prueba de la Historia 6
  sin duplicarlos en reinicios.
- **FR-038**: El README MUST documentar usuario, contraseña, rol y junta de cada usuario de
  prueba.

**Interfaz**

- **FR-039**: Todos los textos visibles MUST estar en español y todas las pantallas MUST ser
  usables en computador y en celular.

### Key Entities *(include if feature involves data)*

- **Junta**: organización atendida por la plataforma; nombre, tipo (RIEGO, PARROQUIAL,
  COMUNITARIA), RUC único de 13 dígitos, dirección, teléfono, provincia, cantón, fecha de
  registro, activa y motivo de suspensión.
- **Usuario**: persona con acceso; nombres, apellidos, cédula, correo, usuario, contraseña,
  rol, activo, debe cambiar contraseña, intentos fallidos y bloqueado hasta. Pertenece a una
  junta (salvo SUPER_ADMIN).
- **Socio**: especialización de Usuario; código de socio, fecha de ingreso, estado (ACTIVO,
  INACTIVO), celular, tiene cuenta. Pertenece a un sector de su junta.
- **Sector**: zona de la junta; nombre y descripción.
- **SolicitudRecuperacion**: pedido de restablecer contraseña; usuario que solicita, junta del
  solicitante, fecha de solicitud, estado (PENDIENTE, ATENDIDA), fecha de atención y quién la
  atendió.
- **Notificacion**: aviso para un usuario; título, mensaje, fecha, leída y enlace.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un usuario con credenciales correctas llega a su pantalla de inicio en menos de
  30 segundos desde que abre la aplicación.
- **SC-002**: El 100% de los intentos de un usuario de una junta de ver o modificar datos de
  otra junta son rechazados sin mostrar datos.
- **SC-003**: El 100% de las combinaciones rol × caso de uso de la matriz de seguridad se
  comportan como indica la matriz (acceso completo, solo lectura o 403).
- **SC-004**: Una recuperación de contraseña (solicitud, atención e ingreso con la temporal)
  se completa en menos de 5 minutos una vez que el administrador ve la solicitud.
- **SC-005**: Todos los mensajes de excepción de CU20, CU21 y CU22 aparecen exactamente como
  están especificados.
- **SC-006**: Todas las pantallas de esta funcionalidad se usan sin desplazamiento horizontal
  en una pantalla de celular de 360 px de ancho.
- **SC-007**: Con los datos de prueba, cada uno de los usuarios documentados inicia sesión al
  primer intento.

## Assumptions

- El panel de inicio de la directiva (CU25), "Mi información" (CU17) y el listado de juntas
  (CU01) se especifican en otras funcionalidades; aquí se muestran pantallas de inicio
  provisionales por rol.
- La lista de "Solicitudes de recuperación" es una página propia accesible desde el menú del
  ADMIN_JUNTA y del SUPER_ADMIN; cuando exista el CU25 se mostrará también como tarjeta.
- El usuario de un socio es su cédula (CU04); el de la directiva es el definido al crear su
  cuenta. La recuperación acepta cualquiera de los dos.
- Una cédula identifica a una sola cuenta en toda la plataforma: una persona pertenece a una
  sola junta y tiene un solo rol (CU23). Por eso la cédula y el usuario son únicos en toda la
  plataforma y la búsqueda de recuperación encuentra como máximo una cuenta.
- Las notificaciones pertenecen a un usuario y se aíslan por destinatario; las solicitudes de
  recuperación guardan además la junta del solicitante para filtrar por junta.
- La recuperación del SUPER_ADMIN queda fuera de la plataforma (soporte técnico).
- El mensaje de bloqueo revela que la cuenta existe; se acepta porque solo aparece tras 5
  intentos sobre esa cuenta.
- Los datos de prueba de predios, eventos y demás módulos se agregan con sus funcionalidades.
- Las notificaciones de otros orígenes (CU08, CU10, CU15, CU18, CU28, CU30) se agregan con
  sus casos de uso, reutilizando la campana.
- No se envían correos: las contraseñas temporales se entregan en persona.
- Tiempo de inactividad de sesión de 30 minutos, valor estándar para aplicaciones web.
