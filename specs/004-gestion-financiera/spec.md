# Feature Specification: Módulo Gestión Financiera

**Feature Branch**: `004-gestion-financiera`

**Created**: 2026-11-22

**Status**: Draft

**Input**: User description: "Módulo Gestión Financiera: casos de uso CU09 a CU14, CU30 y CU31 de docs/modulo-3-financiera.md, con el modelo de docs/modelo-dominio.md. El CU10 usa las inasistencias del módulo administrativo. Cada caso de uso es una historia de usuario; su flujo normal, alternos, excepciones y reglas son los criterios de aceptación. Mockup en docs/mockups/modulo3-CU09-CU14.png."

**Casos de uso cubiertos**: CU09, CU10, CU11, CU12, CU13, CU14, CU30, CU31.

**Depende de**: la funcionalidad 001 (sesión, matriz de permisos, aislamiento por junta,
campana de notificaciones), la 002 (datos de la junta: logo, RUC y monto de multa por
inasistencia; panel de la directiva) y la 003 (socios, sectores, eventos, asistencia cerrada,
consulta de inasistencias para el módulo financiero y correcciones de asistencia). Usa las filas
de la matriz ya existentes para `/pagos`, `/multas`, `/egresos`, `/reportes`, `/inventario`,
`/activos` y `/cuotas`, que hoy muestran "Módulo en construcción", y completa las tarjetas del
panel "Saldo actual", "Deuda por cobrar" e "Ítems con stock bajo".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Emitir cuotas y obligaciones (Priority: P1) — CU30

El Tesorero/a emite cuotas (tarifa anual, consumo de medidor, etc.) a todos los socios activos,
a los de un sector o a un socio, para cobrarlas después.

**Why this priority**: junto con las multas, las cuotas son lo que se cobra en CU09; sin
obligaciones no hay cobranza, deuda ni morosos.

**Independent Test**: como TESORERO, emitir una cuota a todos los socios activos, a un sector y a
un socio, intentar repetir el concepto y anular una cuota sin pagos.

**Acceptance Scenarios**:

1. **Given** un TESORERO en "Cuotas", **When** presiona "Emitir Cuotas", **Then** ve un
   formulario con concepto, monto, fecha de vencimiento y a quién se aplica: "Todos los socios
   activos", "Un sector" (lista de sectores de la junta) o "Un socio" (búsqueda por cédula o
   nombre). (CU30 disparador y flujo)
2. **Given** datos válidos, **When** presiona "Vista previa", **Then** el sistema muestra
   cuántas cuotas va a crear y el total ("Se crearán 45 cuotas por un total de $562.50") antes
   de confirmar. (CU30 flujo)
3. **Given** la vista previa, **When** confirma, **Then** se crean las cuotas PENDIENTES con
   saldo pendiente igual al monto, fecha de emisión de hoy y el mensaje "Cuotas emitidas". (CU30
   flujo)
4. **Given** un monto cero o negativo, **When** intenta emitir, **Then** se rechaza con "El monto
   debe ser mayor que cero". (CU30 excepción)
5. **Given** socios que ya tienen una cuota no anulada con el mismo concepto, **When** emite a
   todos o a un sector, **Then** esos socios se omiten y la vista previa lo indica ("3 socios ya
   tienen esta cuota y se omitirán"); si se emite a un solo socio que ya la tiene, se rechaza con
   "Este socio ya tiene una cuota con ese concepto". (CU30 excepción)
6. **Given** una cuota PENDIENTE sin pagos, **When** el tesorero presiona "Anular" y escribe el
   motivo, **Then** la cuota pasa a ANULADA, guarda el motivo y deja de contar como deuda. (CU30
   alterno)
7. **Given** una cuota con algún pago aplicado (PARCIAL o PAGADA), **Then** no ofrece "Anular" y
   cualquier intento se rechaza con "Una cuota con pagos no se puede anular". (CU30 regla)
8. **Given** un ADMIN_JUNTA, **When** abre "Cuotas", **Then** ve las cuotas emitidas sin poder
   emitir ni anular; los intentos responden 403.

---

### User Story 2 - Registrar ingresos por pagos (Priority: P1) — CU09

El Tesorero/a cobra a un socio las multas y cuotas que tiene pendientes, emite un número de
transacción y un recibo.

**Why this priority**: es el ingreso de dinero de la junta y el caso de uso de prioridad alta del
módulo.

**Independent Test**: como TESORERO, cobrar a un socio dos obligaciones completas, luego un abono
parcial, intentar montos inválidos e imprimir el recibo.

**Acceptance Scenarios**:

1. **Given** un TESORERO en "Pagos" (Cobranzas), **Then** ve la lista de socios de su junta con
   su deuda total y un buscador por cédula o nombre; cada fila tiene el botón "Cobrar" y los
   socios sin deuda muestran el estado "Al día" en verde. (CU09 disparador)
2. **Given** "Cobrar" junto a un socio, **Then** el sistema muestra el socio seleccionado (nombre,
   cédula, sector) y sus obligaciones pendientes (multas y cuotas PENDIENTES o PARCIALES), cada una
   con concepto, vencimiento si tiene, saldo pendiente y un check marcado, el "Total a cobrar" de
   lo marcado, el monto recibido (por defecto igual al total) y el método de pago (Efectivo,
   Transferencia). (CU09 flujo, mockup)
3. **Given** obligaciones marcadas y monto igual al total, **When** presiona "Registrar Pago",
   **Then** el sistema guarda el pago con número de transacción, fecha, método y concepto, deja
   las obligaciones marcadas como PAGADAS con saldo cero, muestra el estado de cuenta actualizado
   (verde "Al día" si ya no queda deuda) y ofrece "Imprimir Recibo". (CU09 flujo)
4. **Given** un monto menor al total marcado y mayor que cero, **When** presiona "Registrar
   Pago", **Then** el sistema pide confirmar "El monto es menor al total. ¿Registrar como abono
   parcial?"; al confirmar, aplica el monto a las obligaciones marcadas empezando por la más
   antigua, las cubiertas quedan PAGADAS, la última alcanzada queda PARCIAL con su saldo restante
   y las demás siguen como estaban. (CU09 alterno)
5. **Given** un monto cero, negativo o mayor al total marcado, o ninguna obligación marcada,
   **When** presiona "Registrar Pago", **Then** se rechaza con "El monto debe ser mayor que cero y
   no superar el total seleccionado" o "Seleccione al menos una obligación". (CU09 excepción)
6. **Given** un pago registrado, **When** presiona "Imprimir Recibo", **Then** ve un recibo con
   el logo, nombre y RUC de la junta, número de transacción, fecha, socio, método, el detalle de
   lo aplicado a cada obligación y el total, listo para imprimir. (CU09 flujo)
7. **Given** cualquier pago, **Then** no existe opción para borrarlo ni editarlo; solo se anula
   con una transacción de reverso (US8). (CU09 regla)
8. **Given** un ADMIN_JUNTA, **When** abre "Pagos", **Then** ve socios, deudas, pagos y recibos
   sin poder cobrar; los intentos responden 403.

---

### User Story 3 - Generar multas por inasistencia (Priority: P1) — CU10

El Tesorero/a convierte en multas las inasistencias (AUSENTE) de los eventos con asistencia
cerrada, con el monto estándar de la junta y la opción de ajustarlo o excluir socios.

**Why this priority**: es la conexión entre el módulo administrativo y el financiero, y la
principal fuente de multas que se cobran en CU09.

**Independent Test**: cerrar la asistencia de una Minga con dos ausentes y un justificado,
sincronizar multas, editar un monto, desmarcar a un socio, aprobar y comprobar que la deuda de
cada socio aumentó y que una segunda sincronización no repite multas.

**Acceptance Scenarios**:

1. **Given** un TESORERO en "Multas", **When** presiona "Sincronizar Multas de Asistencia",
   **Then** ve una tabla con cada inasistencia sin multa de los eventos con asistencia cerrada:
   check marcado, evento (tipo y fecha), socio ausente, estado "AUSENTE" y valor de la multa igual
   al monto estándar de la junta, más el número de inasistencias detectadas. (CU10 flujo, mockup)
2. **Given** la tabla, **When** el tesorero cambia el valor de una fila, **Then** ese valor se
   usa solo para esa multa; un valor cero o negativo se rechaza con "El monto debe ser mayor que
   cero". (CU10 flujo)
3. **Given** la tabla, **When** desmarca a un socio, **Then** ese socio no recibe multa en esta
   aprobación aunque figure como Ausente. (CU10 alterno)
4. **Given** filas marcadas, **When** presiona "Aprobar y Facturar Multas", **Then** se crea una
   multa PENDIENTE por fila marcada (motivo con el tipo y la fecha del evento, monto, fecha de
   hoy, saldo pendiente igual al monto), la deuda de cada socio aumenta y se muestra "Se
   generaron N multas por $X". (CU10 flujo)
5. **Given** socios JUSTIFICADOS o PRESENTES, **Then** nunca aparecen en la tabla. (CU10 regla)
6. **Given** una inasistencia que ya tiene multa (en cualquier estado), **Then** no vuelve a
   aparecer en la tabla y el sistema rechaza crear una segunda multa para ella. (CU10 regla)
7. **Given** Asambleas o Mingas de hoy o anteriores cuya asistencia no está cerrada, **Then**
   aparecen en un bloque aparte, sin checks ni montos, con el aviso "Acta de evento no
   oficializada por Secretaría". (CU10 excepción)
8. **Given** que no hay inasistencias por multar, **Then** la tabla muestra "No hay inasistencias
   pendientes de multar" y el botón de aprobar no está disponible.
9. **Given** un SECRETARIO o ADMIN_JUNTA, **When** abre "Multas", **Then** ve la lista de multas
   generadas y la tabla de sincronización sin poder aprobar; los intentos responden 403.

---

### User Story 4 - Registrar egresos de la junta (Priority: P1) — CU11

El Tesorero/a registra cada salida de dinero con su categoría, proveedor y factura, y el sistema
recalcula el saldo actual.

**Why this priority**: sin egresos no hay saldo ni balance general; es de prioridad alta.

**Independent Test**: registrar un egreso con factura, otro sin factura y adjuntarla después, y
uno que supere el saldo confirmando el registro negativo.

**Acceptance Scenarios**:

1. **Given** un TESORERO en "Egresos", **Then** ve la lista de egresos (fecha, concepto,
   categoría, proveedor, monto, soporte) y el "Saldo Actual"; **When** presiona "Nuevo Gasto",
   **Then** ve un formulario con categoría (Materiales, Honorarios, Servicios básicos,
   Mantenimiento, Otros), concepto, monto, fecha, proveedor o beneficiario y adjunto opcional de
   la factura (PDF, JPG o PNG). (CU11 disparador y flujo, mockup)
2. **Given** datos válidos, **When** presiona "Registrar Salida de Dinero", **Then** el egreso
   se agrega a la lista, el "Saldo Actual" se recalcula (ingresos menos egresos) y aparece
   "Egreso registrado". (CU11 flujo)
3. **Given** un egreso sin factura, **When** se guarda, **Then** queda marcado "Pendiente de
   soporte"; **When** más tarde el tesorero adjunta la factura desde la lista, **Then** la marca
   desaparece y la factura se puede ver. (CU11 alterno)
4. **Given** un monto mayor que el Saldo Actual, **When** presiona "Registrar Salida de Dinero",
   **Then** el sistema pide confirmar "Fondos insuficientes. ¿Forzar registro negativo?"; si
   confirma, se guarda y el saldo queda negativo; si cancela, no se guarda nada. (CU11 excepción)
5. **Given** una categoría que no es una de las predefinidas, un monto cero o negativo, o un
   concepto o fecha vacíos, **Then** se rechaza indicando el campo. (CU11 regla)
6. **Given** un adjunto de otro tipo o mayor a 5 MB, **Then** se rechaza con "El archivo excede
   el límite permitido" o "Formato no permitido. Use PDF, JPG o PNG".
7. **Given** un ADMIN_JUNTA, **When** abre "Egresos", **Then** ve los egresos y sus soportes sin
   poder registrar ni adjuntar; los intentos responden 403.

---

### User Story 5 - Reversar un pago (Priority: P2) — CU31

El Tesorero/a anula un pago equivocado con una transacción de reverso que devuelve la deuda a las
obligaciones que ese pago había cancelado.

**Why this priority**: es la única forma de corregir un pago (CU09 prohíbe borrarlos) y la que
exigen las correcciones de asistencia con multas pagadas.

**Independent Test**: cobrar dos obligaciones a un socio, reversar el pago y comprobar que ambas
vuelven a su saldo anterior, que el pago y el reverso aparecen en el detalle y en el balance, y
que no se puede reversar dos veces.

**Acceptance Scenarios**:

1. **Given** un TESORERO en el detalle de un pago, **When** presiona "Reversar", **Then** el
   sistema pide el motivo (obligatorio) y la confirmación. (CU31 disparador y flujo)
2. **Given** motivo y confirmación, **Then** el sistema crea una transacción de reverso con su
   propio número, fecha y el mismo monto, marca el pago como reversado con el motivo, devuelve a
   cada multa o cuota exactamente lo que ese pago le había aplicado (recalculando su estado:
   PENDIENTE o PARCIAL) y muestra "Pago reversado". (CU31 flujo)
3. **Given** un motivo vacío, **Then** se rechaza con "El motivo es obligatorio". (CU31 flujo)
4. **Given** un pago ya reversado, **Then** no ofrece "Reversar" y un intento se rechaza con
   "Este pago ya fue reversado". (CU31 excepción)
5. **Given** un pago reversado, **Then** sigue visible en la lista de pagos con la marca
   "Reversado", su motivo y el número del reverso, y los reportes muestran el pago y su reverso.
   (CU31 regla)
6. **Given** un ADMIN_JUNTA, **Then** puede ver el pago y su reverso pero no ve "Reversar" y el
   intento responde 403. (CU31 regla: solo el tesorero)

---

### User Story 6 - Gestionar inventario (Priority: P2) — CU13

El Tesorero/a lleva el stock de materiales (tubería, válvulas, medidores) con entradas y salidas
registradas en un kardex.

**Why this priority**: controla bienes de consumo de la junta; prioridad media.

**Independent Test**: crear un ítem, registrar una entrada y una salida, intentar una salida
mayor al stock, ver el kardex y la advertencia de stock bajo, e inactivar el ítem.

**Acceptance Scenarios**:

1. **Given** un TESORERO en "Inventario", **Then** ve los ítems con código, nombre, unidad de
   medida, stock y estado ("OK" o "CRÍTICO"), y el botón "Nuevo Movimiento de Inventario".
   (CU13 disparador y flujo, mockup)
2. **Given** "Nuevo Ítem", **When** ingresa nombre, unidad de medida y stock mínimo (cero o más),
   **Then** el ítem se crea ACTIVO con stock cero y un código generado. (CU13 flujo)
3. **Given** "Nuevo Movimiento de Inventario", **When** elige un ítem activo, Entrada o Salida,
   una cantidad mayor que cero y un motivo, **Then** el stock se actualiza, el movimiento queda
   en el kardex del ítem (fecha, tipo, cantidad, motivo, stock resultante, usuario) y aparece
   "Movimiento registrado". (CU13 flujo)
4. **Given** un ítem cuyo stock queda igual o por debajo del mínimo, **Then** su fila se
   resalta con "CRÍTICO" sin bloquear ninguna operación. (CU13 alterno)
5. **Given** una salida mayor al stock, **Then** se rechaza con "Stock insuficiente para esta
   salida" y el stock no cambia. (CU13 excepción)
6. **Given** un ítem con movimientos, **Then** no se puede eliminar; solo "Inactivar" (y
   "Reactivar"); un ítem INACTIVO no se ofrece para movimientos nuevos y conserva su kardex.
   Un ítem sin movimientos sí se puede eliminar. (CU13 regla)
7. **Given** un nombre de ítem que ya existe en la junta (sin distinguir mayúsculas), **Then** se
   rechaza con "Ya existe un ítem con ese nombre".
8. **Given** un ADMIN_JUNTA, **When** abre "Inventario", **Then** ve ítems y kardex sin poder
   cambiar nada; los intentos responden 403.

---

### User Story 7 - Gestionar activos fijos (Priority: P2) — CU14

El Tesorero/a registra los bienes duraderos de la junta (bombas, vehículos, infraestructura) y el
sistema calcula su depreciación y el patrimonio.

**Why this priority**: forma parte del patrimonio de la junta; prioridad media.

**Independent Test**: registrar un activo de cada categoría con fechas de adquisición pasadas,
comprobar la depreciación acumulada y el patrimonio, intentar un valor cero y dar uno de baja.

**Acceptance Scenarios**:

1. **Given** un TESORERO en "Activos fijos", **Then** ve la lista de activos (código, nombre,
   categoría, fecha de adquisición, valor de compra, depreciación acumulada, valor en libros,
   estado) y el total del patrimonio activo; **When** presiona "Registrar Activo", **Then** ve la
   ficha con nombre, categoría (Maquinaria, Infraestructura, Equipo de oficina, Vehículos), fecha
   de adquisición, valor de compra, estado (Nuevo, Usado) y foto o acta de entrega opcional (JPG,
   PNG o PDF). (CU14 disparador y flujo, mockup)
2. **Given** datos válidos, **When** presiona "Guardar Activo", **Then** el activo recibe un
   código generado, se calcula su depreciación acumulada y su valor se suma al patrimonio.
   (CU14 flujo)
3. **Given** un valor de compra vacío, cero o negativo, **Then** el botón "Guardar Activo" se
   deshabilita y, si se envía igual, el sistema lo rechaza con "El valor de compra debe ser mayor
   que cero". (CU14 excepción)
4. **Given** un valor de compra mayor a $200 sin categoría, **Then** se rechaza con "Los activos
   de más de $200 deben tener categoría". (CU14 regla)
5. **Given** un activo con categoría, **Then** su depreciación acumulada es lineal: valor de
   compra × meses completos desde la adquisición ÷ (vida útil en años × 12), sin superar el valor
   de compra, con vida útil Infraestructura 20 años, Maquinaria 10, Equipo de oficina 10 y
   Vehículos 5; un activo sin categoría (de $200 o menos) no se deprecia. (CU14 regla)
6. **Given** un activo activo, **When** presiona "Dar de Baja" y escribe el motivo, **Then** pasa
   a DADO_DE_BAJA, sale del total del patrimonio, conserva sus datos y muestra la fecha y el
   motivo de la baja. (CU14 alterno)
7. **Given** una fecha de adquisición futura, **Then** se rechaza con "La fecha de adquisición no
   puede ser futura".
8. **Given** un ADMIN_JUNTA, **When** abre "Activos fijos", **Then** ve los activos y el
   patrimonio sin poder registrar ni dar de baja; los intentos responden 403.

---

### User Story 8 - Emitir reportes financieros (Priority: P3) — CU12

El Tesorero/a o el Administrador generan el Balance General de Ingresos y Egresos y el reporte de
Socios Morosos de un rango de fechas, en PDF o Excel.

**Why this priority**: resume la información de las demás historias; depende de que existan
pagos, egresos y obligaciones.

**Independent Test**: con pagos, un reverso, egresos y cuotas vencidas cargados, generar ambos
reportes en PDF y en Excel y un rango sin movimientos.

**Acceptance Scenarios**:

1. **Given** un TESORERO o ADMIN_JUNTA en "Reportes", **Then** ve el formulario con Tipo de
   Reporte (Balance General de Ingresos y Egresos, Socios Morosos), fecha de inicio, fecha de fin
   y formato (PDF, Excel), y una vista previa con el saldo del rango. (CU12 disparador, mockup)
2. **Given** "Balance General" y un rango, **When** presiona "Generar Reporte", **Then** se
   descarga el archivo con los ingresos (pagos, con sus reversos como valores negativos), los
   egresos por categoría y el saldo del rango (ingresos netos menos egresos). (CU12 flujo)
3. **Given** "Socios Morosos" y un rango, **When** genera, **Then** el archivo lista a los socios
   con deuda vencida acumulada hasta la fecha de fin: cuotas con saldo pendiente cuyo vencimiento
   es anterior a hoy y no posterior a la fecha de fin, y multas con saldo pendiente con fecha no
   posterior a la fecha de fin, sin importar la fecha de inicio (la mora se acumula), con el
   detalle de cada obligación y la deuda total por socio. (CU12 flujo, CU30 regla)
4. **Given** un rango sin movimientos para el reporte elegido, **Then** no se descarga nada y se
   muestra "No existen registros en el rango seleccionado". (CU12 excepción)
5. **Given** el formato PDF, **Then** el archivo lleva el logo (si la junta tiene), el nombre y el
   RUC de la junta, el tipo de reporte, el rango y la fecha de generación. (CU12 regla)
6. **Given** una fecha de fin anterior a la de inicio, **Then** se rechaza con "La fecha de fin
   debe ser igual o posterior a la de inicio".
7. **Given** un SECRETARIO, **When** intenta abrir "Reportes", **Then** recibe 403.

---

### Edge Cases

- Un socio inactivo conserva su deuda: sigue apareciendo en Cobranzas si debe, puede pagar y
  aparece en Socios Morosos; no recibe cuotas emitidas a "todos los socios activos" ni a un
  sector, pero sí a él de forma individual.
- Si una corrección de asistencia (CU28) aprobada cambia a un socio de AUSENTE a JUSTIFICADO o
  PRESENTE y la inasistencia ya tiene multa: si la multa no tiene pagos se anula automáticamente
  con el motivo "Asistencia corregida"; si tiene pagos, se notifica a los tesoreros activos de la
  junta para que reversen el pago (CU31) y luego puedan tratar la multa.
- Si una corrección cambia a un socio de PRESENTE o JUSTIFICADO a AUSENTE, esa inasistencia
  aparece en la siguiente sincronización de multas.
- Si cambia el monto de multa por inasistencia de la junta, solo afecta a las sincronizaciones
  siguientes; las multas ya creadas no cambian.
- Un pago reversado no se vuelve a reversar ni se "des-reversa"; si el cobro era correcto, se
  registra un pago nuevo.
- Una multa ANULADA no se cobra, no suma deuda y no se reporta como morosa.
- Dos tesoreros cobran al mismo socio a la vez: el segundo pago se rechaza si el monto supera el
  saldo que queda en ese momento ("Las obligaciones cambiaron; vuelva a cargar el cobro").
- Una cuota con fecha de vencimiento pasada no cambia de estado: sigue PENDIENTE o PARCIAL y se
  considera vencida en reportes y en Cobranzas.
- Los montos usan dos decimales y dólares; montos con más de dos decimales se rechazan.
- El Saldo Actual considera todos los pagos no reversados menos todos los egresos de la junta,
  desde el inicio; los reportes usan solo el rango elegido.
- Un ítem de inventario o un activo de otra junta, o un pago, cuota o multa de otra junta,
  responden como no encontrados.

## Requirements *(mandatory)*

### Functional Requirements

**Cuotas (CU30)**

- **FR-001** (CU30): El TESORERO MUST poder emitir cuotas con concepto, monto mayor que 0, fecha
  de vencimiento y destino (todos los socios ACTIVOS, los ACTIVOS de un sector de la junta o un
  socio de la junta); cada cuota nace PENDIENTE con saldo pendiente igual al monto.
- **FR-002** (CU30): Antes de crear, el sistema MUST mostrar cuántas cuotas creará y el total, y
  crear solo al confirmar.
- **FR-003** (CU30): Un socio MUST NOT tener dos cuotas no anuladas con el mismo concepto (sin
  distinguir mayúsculas ni espacios extremos); en emisiones masivas los socios que ya la tienen
  se omiten e informan.
- **FR-004** (CU30): El TESORERO MUST poder anular con motivo obligatorio una cuota sin pagos
  aplicados; una cuota con pagos MUST NOT anularse.

**Pagos (CU09)**

- **FR-005** (CU09): Cobranzas MUST listar a los socios de la junta con su deuda total (suma de
  saldos de multas y cuotas no anuladas) con búsqueda por cédula o nombre, mostrando "Al día" a
  quienes no deben.
- **FR-006** (CU09): Al cobrar, el sistema MUST listar las multas y cuotas PENDIENTES o PARCIALES
  del socio con check, calcular el total marcado y permitir elegir método (EFECTIVO,
  TRANSFERENCIA) y monto.
- **FR-007** (CU09): El monto MUST ser mayor que 0 y no mayor que el total marcado; si es menor,
  el sistema MUST pedir confirmación de abono parcial y aplicarlo a las obligaciones marcadas de
  la más antigua a la más reciente.
- **FR-008** (CU09): Cada pago MUST guardar número de transacción único en la junta, fecha,
  método, concepto, socio, tesorero que cobró y el detalle de cuánto aplicó a cada obligación;
  las obligaciones MUST actualizar saldo y estado (PARCIAL o PAGADA).
- **FR-009** (CU09): Tras registrar, el sistema MUST mostrar el estado de cuenta del socio ("Al
  día" en verde si no queda deuda) y ofrecer un recibo imprimible con logo, nombre y RUC de la
  junta.
- **FR-010** (CU09): Un pago MUST NOT editarse ni eliminarse.

**Multas (CU10)**

- **FR-011** (CU10): La sincronización MUST obtener las inasistencias (AUSENTE) de los eventos
  con asistencia cerrada de la junta mediante la consulta que ofrece el módulo administrativo, y
  mostrar solo las que no tienen multa, con el monto estándar de la junta editable por fila.
- **FR-012** (CU10): Al aprobar, el sistema MUST crear una multa PENDIENTE por cada fila marcada
  (motivo con tipo y fecha del evento, monto > 0, saldo pendiente igual al monto) ligada a su
  asistencia, en una sola operación.
- **FR-013** (CU10): Una asistencia MUST NOT generar más de una multa; los JUSTIFICADOS y
  PRESENTES MUST NOT aparecer ni multarse.
- **FR-014** (CU10): Las Asambleas y Mingas PROGRAMADAS de hoy o anteriores sin asistencia
  cerrada MUST mostrarse bloqueadas con "Acta de evento no oficializada por Secretaría".
- **FR-015** (CU28, CU10): Al aprobarse una corrección de asistencia que saca a un socio de
  AUSENTE, su multa MUST anularse automáticamente si no tiene pagos; si tiene pagos, el sistema
  MUST notificar a los TESORERO activos de la junta para que reversen el pago.

**Egresos (CU11)**

- **FR-016** (CU11): El TESORERO MUST poder registrar egresos con categoría predefinida
  (MATERIALES, HONORARIOS, SERVICIOS_BASICOS, MANTENIMIENTO, OTROS), concepto, monto > 0, fecha,
  proveedor opcional y comprobante opcional (PDF, JPG o PNG de hasta 5 MB).
- **FR-017** (CU11): Un egreso sin comprobante MUST quedar "Pendiente de soporte" hasta que se le
  adjunte uno.
- **FR-018** (CU11): El "Saldo Actual" MUST ser la suma de pagos no reversados menos la suma de
  egresos de la junta; si un egreso lo supera, el sistema MUST pedir confirmar "Fondos
  insuficientes. ¿Forzar registro negativo?" y guardar solo si se confirma.

**Reversos (CU31)**

- **FR-019** (CU31): El TESORERO MUST poder reversar un pago con motivo obligatorio y
  confirmación; el reverso MUST tener su propio número de transacción, fecha y el mismo monto.
- **FR-020** (CU31): Al reversar, cada obligación MUST recuperar exactamente el monto que ese pago
  le aplicó y recalcular su estado; el pago original MUST quedar marcado como reversado con su
  motivo y no borrarse.
- **FR-021** (CU31): Un pago reversado MUST NOT reversarse otra vez; solo el TESORERO MUST poder
  reversar.

**Reportes (CU12)**

- **FR-022** (CU12): El TESORERO y el ADMIN_JUNTA MUST poder generar el Balance General de
  Ingresos y Egresos y el reporte de Socios Morosos para un rango de fechas válido, en PDF o
  Excel, que se descarga.
- **FR-023** (CU12): El Balance MUST mostrar pagos y reversos del rango (el reverso como valor
  negativo), egresos por categoría y saldo del rango; Socios Morosos MUST listar socios con
  deuda vencida acumulada hasta la fecha de fin (cuotas con vencimiento anterior a hoy y no
  posterior a la fecha de fin, y multas con fecha no posterior a la fecha de fin, ambas con saldo
  pendiente y no anuladas) y su deuda total.
- **FR-024** (CU12): Si el rango no tiene registros para el reporte, el sistema MUST mostrar "No
  existen registros en el rango seleccionado" y no generar archivo.
- **FR-025** (CU12): Los PDF MUST llevar logo (si existe), nombre y RUC de la junta, tipo de
  reporte, rango y fecha de generación.

**Inventario (CU13)**

- **FR-026** (CU13): El TESORERO MUST poder crear ítems con nombre único en la junta (sin
  distinguir mayúsculas), unidad de medida y stock mínimo ≥ 0; el ítem nace ACTIVO, con stock 0 y
  código generado.
- **FR-027** (CU13): El TESORERO MUST poder registrar movimientos de ENTRADA o SALIDA con cantidad
  > 0 y motivo sobre ítems ACTIVOS; cada movimiento actualiza el stock y queda en el kardex con
  fecha, stock resultante y usuario.
- **FR-028** (CU13): Una SALIDA mayor al stock MUST rechazarse con "Stock insuficiente para esta
  salida"; un ítem con stock igual o menor al mínimo MUST resaltarse como "CRÍTICO" sin bloquear.
- **FR-029** (CU13): Un ítem con movimientos MUST NOT eliminarse, solo inactivarse o
  reactivarse; uno sin movimientos sí se puede eliminar.

**Activos fijos (CU14)**

- **FR-030** (CU14): El TESORERO MUST poder registrar activos con nombre, categoría, fecha de
  adquisición no futura, valor de compra > 0, estado (NUEVO, USADO) y archivo opcional (JPG, PNG o
  PDF de hasta 5 MB); el activo recibe un código generado. El botón de guardar MUST
  deshabilitarse mientras el valor no sea mayor que 0 y el servidor MUST validarlo igualmente.
- **FR-031** (CU14): Un activo de más de $200 MUST tener categoría.
- **FR-032** (CU14): La depreciación acumulada MUST calcularse en línea recta por meses completos
  según la vida útil de la categoría (Infraestructura 20 años, Maquinaria 10, Equipo de oficina
  10, Vehículos 5), sin superar el valor de compra; el patrimonio MUST ser la suma de los valores
  en libros de los activos no dados de baja.
- **FR-033** (CU14): El TESORERO MUST poder dar de baja un activo con motivo obligatorio; pasa a
  DADO_DE_BAJA, sale del patrimonio y conserva su historial.

**Transversales**

- **FR-034**: Los permisos MUST seguir la matriz vigente: TESORERO con acceso completo a pagos,
  multas, egresos, reportes, inventario, activos y cuotas; ADMIN_JUNTA solo lectura en todos
  ellos salvo reportes (completo) y sin reversar; SECRETARIO solo lectura en multas y sin acceso a
  los demás.
- **FR-035**: Todo dato de esta funcionalidad MUST pertenecer a una junta y solo verse o
  modificarse desde esa junta; los identificadores de otra junta responden como no encontrados.
- **FR-036** (CU25): Las tarjetas del panel "Saldo actual" (FR-018), "Deuda por cobrar" (suma de
  saldos pendientes de multas y cuotas no anuladas) e "Ítems con stock bajo" (ítems ACTIVOS con
  stock ≤ mínimo) MUST mostrar valores reales.
- **FR-037**: Los montos MUST manejarse en dólares con dos decimales y mostrarse con el símbolo
  "$".
- **FR-038**: Todos los textos MUST estar en español y las pantallas MUST usarse en celular sin
  desplazamiento horizontal de la página.

### Key Entities *(include if feature involves data)*

- **Cuota** (nueva): concepto, monto, fecha de emisión, fecha de vencimiento, estado (PENDIENTE,
  PARCIAL, PAGADA, ANULADA), saldo pendiente, motivo de anulación; pertenece a un socio y a una
  junta.
- **Multa** (nueva): motivo, monto, fecha, estado (PENDIENTE, PARCIAL, PAGADA, ANULADA), saldo
  pendiente; pertenece a un socio y a una junta, y se origina en una asistencia (como máximo una
  multa por asistencia).
- **Pago** (nuevo): número de transacción, monto, fecha, concepto, método (EFECTIVO,
  TRANSFERENCIA), reversado, motivo del reverso; lo realiza un socio y lo registra un tesorero.
- **Aplicación de pago** (nueva): cuánto de un pago se aplicó a una multa o cuota concreta;
  permite el recibo detallado y el reverso exacto.
- **Reverso** (nuevo): número de transacción, fecha, monto, motivo y tesorero; anula un pago.
- **Egreso** (nuevo): concepto, monto, fecha, categoría, proveedor, comprobante, pendiente de
  soporte.
- **Ítem de inventario** (nuevo): código, nombre, unidad de medida, stock, stock mínimo, estado
  (ACTIVO, INACTIVO).
- **Movimiento de inventario** (nuevo): tipo (ENTRADA, SALIDA), cantidad, motivo, fecha, stock
  resultante y usuario; forma el kardex del ítem.
- **Activo fijo** (nuevo): código, nombre, categoría, fecha de adquisición, valor de compra,
  estado (NUEVO, USADO, DADO_DE_BAJA), archivo, motivo y fecha de baja.
- **Asistencia** (existente, 003): una inasistencia puede tener como máximo una multa.
- **Junta** (existente): aporta logo, nombre, RUC y monto de multa por inasistencia.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El tesorero cobra dos obligaciones a un socio e imprime el recibo en menos de 1
  minuto.
- **SC-002**: La sincronización y aprobación de multas de un evento con 200 socios se completa
  en menos de 2 minutos y la pantalla responde al aprobar en menos de 3 segundos.
- **SC-003**: El 100% de las inasistencias con multa no vuelven a multarse, y el 100% de los
  socios justificados quedan sin multa.
- **SC-004**: Tras cualquier secuencia de pagos y reversos, el saldo de cada obligación es igual
  a su monto menos lo aplicado por pagos no reversados, y el Saldo Actual coincide con el
  Balance General desde el inicio.
- **SC-005**: El 100% de los pagos registrados permanecen visibles; ningún pago se borra.
- **SC-006**: Un reporte de un mes con 500 movimientos se genera y descarga en menos de 10
  segundos.
- **SC-007**: El 100% de los intentos de ver o modificar datos financieros de otra junta son
  rechazados sin mostrar datos.
- **SC-008**: Todas las pantallas del módulo se usan sin desplazamiento horizontal en un celular
  de 360 px.

## Assumptions

- "Cobranzas" del CU09 y CU30 es la opción de menú "Pagos" (`/pagos`); la emisión y anulación de
  cuotas está en "Cuotas" (`/cuotas`), y el reverso (CU31) se hace desde el detalle del pago.
- El monto recibido se escribe en el cobro (por defecto el total marcado); el mockup solo muestra
  el total, pero el abono parcial del CU09 exige poder ingresar un monto menor.
- El abono parcial se aplica de la obligación más antigua a la más reciente (por fecha de
  vencimiento, o de emisión si no tiene); no se reparte proporcionalmente.
- No se registran pagos mayores a la deuda ni saldos a favor del socio.
- El número de transacción es correlativo por junta y los reversos usan la misma numeración.
- El recibo es una página imprimible desde el navegador, no un archivo descargado.
- La tabla de multas muestra todas las inasistencias sin multa de los eventos cerrados (no solo
  las recientes), de la más reciente a la más antigua; desmarcar a un socio lo excluye solo de
  esa aprobación, así que puede multarse después si corresponde.
- Las multas solo nacen de inasistencias (CU10); no hay multas manuales en este módulo, y una
  multa solo se anula por una corrección de asistencia (FR-015).
- El monto de multa estándar es el "monto de multa por inasistencia" configurado en CU03; si la junta no
  lo configuró, la tabla propone $0.00 y obliga a escribir un monto mayor que cero.
- Un activo de $200 o menos puede quedar sin categoría (regla de CU14) y entonces no se deprecia.
- La depreciación se calcula al mostrar (no se guarda) y no considera valor residual.
- Los comprobantes de egresos y archivos de activos se guardan como los demás archivos de la
  plataforma (tamaño máximo 5 MB); el mockup admite PDF, JPG y PNG.
- El Excel de reportes es una hoja de cálculo con las mismas columnas que el PDF, sin logo.
- Los socios no ven aún su deuda ni sus pagos; eso llega con "Mi información" (CU17) en el
  módulo de comunicaciones.
- No se envían notificaciones a los socios por cuotas, multas o pagos; la única notificación
  nueva es al tesorero por multas pagadas de asistencias corregidas (FR-015).
- El inventario es una sola bodega por junta ("Bodega central" del mockup).
