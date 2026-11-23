---

description: "Lista de tareas de la funcionalidad 004-gestion-financiera"
---

# Tasks: Módulo Gestión Financiera

**Input**: Design documents from `/specs/004-gestion-financiera/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: incluidas. La constitución (Principio VII) exige verificar cada CU contra sus
criterios de aceptación; `plan.md` define una clase de prueba por CU. Se escriben antes de
implementar cada historia y deben fallar primero.

**Organization**: tareas agrupadas por historia de `spec.md`; cada tarea indica su CU
(Principio VIII). Se reutilizan 001, 002 y 003 sin duplicarlas.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia de `spec.md` (US1 a US8)

## Path Conventions

- Código: `src/main/java/ec/uta/multijunta/` (abreviado `JAVA/`)
- Recursos: `src/main/resources/` (abreviado `RES/`)
- Pruebas: `src/test/java/ec/uta/multijunta/` (abreviado `TEST/`)
- Sin comentarios en el código (Principio VI); textos en español (Principio IV)

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Agregar a `pom.xml` las dependencias `com.github.librepdf:openpdf:2.0.5` y `org.apache.poi:poi-ooxml:5.5.1` (research.md R1) y comprobar que `.\mvnw.cmd test` compila
- [X] T002 [P] Agregar a `RES/messages.properties` todas las claves de `specs/004-gestion-financiera/contracts/mensajes.md`
- [X] T003 [P] Agregar a `RES/static/css/app.css` estilos de montos alineados a la derecha, fila "CRÍTICO" resaltada, estado "Al día" en verde y recibo imprimible (`@media print` que oculta menú y botones)

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de terminar esta fase

- [X] T004 [P] Crear enums `JAVA/model/EstadoObligacion.java`, `JAVA/model/MetodoPago.java`, `JAVA/model/CategoriaEgreso.java`, `JAVA/model/EstadoItem.java`, `JAVA/model/TipoMovimiento.java`, `JAVA/model/CategoriaActivo.java` (con vida útil en años) y `JAVA/model/EstadoActivo.java` con los valores y etiquetas de data-model.md (Enums)
- [X] T005 [P] Crear `JAVA/model/Multa.java` y `JAVA/repository/MultaRepository.java` con data-model.md Multa.junta, Multa.socio, Multa.asistenciaId (índice único), Multa.eventoId, Multa.tipoEvento, Multa.fechaEvento, Multa.motivo, Multa.monto, Multa.fecha, Multa.estado, Multa.saldoPendiente, Multa.motivoAnulacion y Multa.version (`@Version`); consultas por junta, por socio con estado PENDIENTE o PARCIAL, `existsByAsistenciaId`, `findByAsistenciaIdAndEstadoNot` y suma de saldos no anulados por junta y por socio
- [X] T006 [P] Crear `JAVA/model/Cuota.java` y `JAVA/repository/CuotaRepository.java` con data-model.md Cuota.junta, Cuota.socio, Cuota.concepto, Cuota.conceptoNormalizado, Cuota.monto, Cuota.fecha, Cuota.fechaVencimiento, Cuota.estado, Cuota.saldoPendiente, Cuota.motivoAnulacion y Cuota.version (`@Version`); consultas por junta con filtro, por socio con estado PENDIENTE o PARCIAL, existencia por socio y concepto normalizado con estado distinto de ANULADA, y suma de saldos no anulados por junta y por socio
- [X] T007 [P] Agregar a `JAVA/service/ArchivoService.java` `validarDocumento(archivo, campo)` que acepta PDF (`%PDF`), PNG y JPG por bytes mágicos hasta 5 MB con las claves `archivo.grande` y `archivo.documentoTipo`, y las subcarpetas `egresos` y `activos` (research.md R8); mantener verdes las pruebas de logos y predios
- [X] T008 [P] Ampliar `TEST/Fabrica.java` con `tesorero(junta)`, `cuota(socio, concepto, monto, vencimiento)`, `multa(socio, monto)` y `multa(socio, monto, saldo)` (con `asistenciaId` ficticio único; la segunda simula pagos vigentes), `cerrarAsistencia(evento, Map<Socio, EstadoAsistencia>)` vía `AsistenciaService` y `pdfMinimo()` / `pngMinimo()` para adjuntos
- [X] T009 Cambiar `moduloEnConstruccion` en `TEST/PermisosPorRolTest.java` para usar `/convocatorias` con SECRETARIO (research.md R12); `soloLecturaNoModifica` sigue con POST a `/pagos` como ADMIN_JUNTA. El retiro de cada ruta de `JAVA/rest/EnConstruccionController.java` se hace en la historia que la construye (T015, T023, T030, T037, T049, T052, T059)

**Checkpoint**: compila y las pruebas de 001 a 003 siguen en verde

---

## Phase 3: User Story 1 - Emitir cuotas y obligaciones (Priority: P1) 🎯 MVP — CU30

**Goal**: el TESORERO emite cuotas a todos, a un sector o a un socio con vista previa, y anula
las que no tienen pagos.

**Independent Test**: `CuotaTest` en verde; pasos 1 a 3 de quickstart.md.

### Tests for User Story 1 ⚠️

- [X] T010 [P] [US1] Escribir `TEST/CuotaTest.java` (CU30) con HU1-1 a HU1-8 de spec.md: formulario con los tres destinos, vista previa con conteo y total sin guardar, emisión a todos los ACTIVOS (sin inactivos), a un sector y a un socio, monto cero/negativo y con más de dos decimales, concepto repetido (omitidos en masivo, rechazo individual, sin distinguir mayúsculas), anulación con motivo y rechazo con motivo vacío, rechazo de anular una cuota PARCIAL o PAGADA (saldo menor que el monto), lectura para ADMIN_JUNTA con 403 al emitir o anular, cuota o socio de otra junta → 404

### Implementation for User Story 1

- [X] T011 [P] [US1] Crear `JAVA/service/DatosCuota.java` y `JAVA/rest/formulario/CuotaFormulario.java` (destino TODOS/SECTOR/SOCIO, sectorId, socioId) con validaciones de data-model.md Cuota.concepto, Cuota.monto y Cuota.fechaVencimiento
- [X] T012 [US1] Agregar a `JAVA/repository/SocioRepository.java` la consulta de socios ACTIVOS de un sector de la junta (la de todos los ACTIVOS ya existe de 003)
- [X] T013 [US1] Crear `JAVA/service/CuotaService.java` (CU30): `previa(datos)` que devuelve destinatarios, omitidos (data-model.md Cuota.conceptoNormalizado) y total; `emitir(datos)` que crea data-model.md Cuota.estado PENDIENTE, Cuota.saldoPendiente = monto y Cuota.fecha = hoy; `anular(id, motivo)` que exige data-model.md Cuota.motivoAnulacion y rechaza con `cuota.conPagos` si Cuota.saldoPendiente es menor que Cuota.monto (tiene pagos vigentes); sector y socio siempre de la junta actual
- [X] T014 [US1] Crear `JAVA/rest/CuotaController.java` con las rutas `/cuotas` de contracts/rutas-ui.md (lista, previa, emitir, anular)
- [X] T015 [US1] Crear `RES/templates/cuotas/lista.html` (filtro, tabla responsive, "Anular" solo para TESORERO en cuotas sin pagos, modal con motivo) y `RES/templates/cuotas/emitir.html` (formulario, resumen de vista previa y botón de confirmar); quitar `/cuotas` de `JAVA/rest/EnConstruccionController.java`
- [X] T016 [US1] Ejecutar `TEST/CuotaTest.java` y corregir hasta que pase

**Checkpoint**: la junta tiene obligaciones que cobrar

---

## Phase 4: User Story 2 - Registrar ingresos por pagos (Priority: P1) — CU09

**Goal**: el TESORERO cobra multas y cuotas, con abono parcial, número de transacción y recibo.

**Independent Test**: `CobranzaTest` en verde; pasos 6 a 8 de quickstart.md.

### Tests for User Story 2 ⚠️

- [X] T017 [P] [US2] Escribir `TEST/CobranzaTest.java` (CU09) con HU2-1 a HU2-8 de spec.md: lista de cobranzas con deuda y "Al día", búsqueda, formulario de cobro con obligaciones PENDIENTES y PARCIALES (sin ANULADAS ni PAGADAS), pago completo (número `TX-000001` correlativo por junta, aplicaciones, estados PAGADA y saldo 0), abono parcial sin confirmar (vuelve con aviso y no guarda) y confirmado (orden de la más antigua), monto cero/negativo/mayor al total y ninguna obligación, recibo con nombre, RUC y detalle, ausencia de rutas de edición o borrado, lectura para ADMIN_JUNTA con 403 al cobrar, socio o pago de otra junta → 404, obligación de otro socio en la selección → rechazo; monto con tres decimales rechazado con `monto.decimales` (FR-037)

### Implementation for User Story 2

- [X] T018 [P] [US2] Crear `JAVA/model/Pago.java`, `JAVA/model/AplicacionPago.java`, `JAVA/repository/PagoRepository.java` y `JAVA/repository/AplicacionPagoRepository.java` con data-model.md Pago.junta, Pago.socio, Pago.numero (único por junta), Pago.monto, Pago.fecha, Pago.concepto, Pago.metodo, Pago.registradoPor, Pago.reversado, Pago.aplicaciones y AplicacionPago.pago, AplicacionPago.multa, AplicacionPago.cuota, AplicacionPago.monto; consultas de pagos por junta con búsqueda, conteo por junta, suma de pagos no reversados por junta y "existe aplicación vigente" por multa y por cuota
- [X] T019 [P] [US2] Crear `JAVA/service/DatosCobro.java` y `JAVA/rest/formulario/CobroFormulario.java` (obligaciones `M-{id}`/`C-{id}`, monto, metodo, confirmarParcial) con validaciones de data-model.md Pago.monto y Pago.metodo
- [X] T020 [US2] Crear `JAVA/service/FinanzasService.java` con `deudaDeSocio`, `deudaPorCobrar` (data-model.md Consultas agregadas) y `saldoActual` (por ahora solo pagos; los egresos se suman en T034); crear `JAVA/service/CobranzaService.java` (CU09): `socios(texto)` con deuda, `obligaciones(socioId)`, `cobrar(socioId, datos)` que valida selección y monto (FR-007), exige confirmación de parcial, aplica por antigüedad (research.md R3), genera data-model.md Pago.numero (research.md R2), arma Pago.concepto y actualiza Multa.saldoPendiente, Multa.estado, Cuota.saldoPendiente y Cuota.estado; traduce el conflicto de versión a `pago.cambio` (research.md R4)
- [X] T021 [US2] Crear `JAVA/rest/CobranzaController.java` con las rutas `/pagos` de contracts/rutas-ui.md (cobranzas, cobrar GET/POST, historial, detalle, recibo)
- [X] T022 [US2] Crear `RES/templates/pagos/cobranzas.html`, `RES/templates/pagos/cobro.html` (socio seleccionado, obligaciones con check, total, monto, método, aviso de parcial con "Confirmar"), `RES/templates/pagos/detalle.html` (aplicaciones, estado de cuenta, "Imprimir Recibo"), `RES/templates/pagos/historial.html` y `RES/templates/pagos/recibo.html` (logo, nombre y RUC de la junta)
- [X] T023 [US2] Quitar `/pagos` de `JAVA/rest/EnConstruccionController.java` y ejecutar `TEST/CobranzaTest.java` hasta que pase

**Checkpoint**: la junta cobra y emite recibos

---

## Phase 5: User Story 3 - Generar multas por inasistencia (Priority: P1) — CU10

**Goal**: el TESORERO convierte inasistencias de eventos cerrados en multas; las correcciones
de asistencia las anulan o avisan.

**Independent Test**: `MultaTest` en verde; pasos 4, 5 y 14 de quickstart.md.

### Tests for User Story 3 ⚠️

- [X] T024 [P] [US3] Escribir `TEST/MultaTest.java` (CU10) con HU3-1 a HU3-9 de spec.md: tabla con ausentes de eventos cerrados y monto de la junta, sin JUSTIFICADOS ni PRESENTES, monto editado por fila y monto inválido, socio desmarcado sin multa, aprobación con mensaje de cantidad y total y deuda aumentada, segunda sincronización sin repetidos y rechazo de duplicado forzado, bloque "Acta de evento no oficializada por Secretaría" para eventos sin cerrar, tabla vacía, junta sin monto de multa configurado (propone $0.00 y exige escribir un monto mayor que cero), lectura para SECRETARIO y ADMIN_JUNTA con 403 al aprobar, eventos de otra junta ausentes; efecto de CU28 (FR-015): corrección aprobada AUSENTE→JUSTIFICADO anula la multa sin pagos y, con pago vigente, notifica a los tesoreros activos sin anular; monto con tres decimales rechazado con `monto.decimales` (FR-037)
- [X] T025 [US3] Agregar a `JAVA/service/IAsistencia.java` el método `eventosSinCerrar(Long juntaId)` y crear el record `JAVA/service/EventoSinCerrar.java`; implementarlo en `JAVA/service/AsistenciaService.java` (contracts/iasistencia.md)
- [X] T026 [US3] Crear el record `JAVA/service/AsistenciaCorregida.java` y publicarlo con `ApplicationEventPublisher` desde `JAVA/service/CorreccionAsistenciaService.java` al aprobar (research.md R6); mantener verde `TEST/CorreccionAsistenciaTest.java`
- [X] T027 [US3] Crear `JAVA/service/MultaService.java` (CU10) que usa solo `IAsistencia`: `pendientes(juntaId)` excluyendo las que ya tienen multa por data-model.md Multa.asistenciaId, `eventosSinCerrar`, `aprobar(seleccion, montos)` que crea en lote data-model.md Multa.motivo (`multa.motivo`), Multa.monto (> 0, por defecto el monto de la junta o 0.00 si no está configurado), Multa.fecha, Multa.estado PENDIENTE y Multa.saldoPendiente, y rechaza duplicados con `multa.duplicada`; `@EventListener` de `AsistenciaCorregida` que fija Multa.estado ANULADA, Multa.saldoPendiente 0 y Multa.motivoAnulacion "Asistencia corregida" cuando Multa.saldoPendiente es igual a Multa.monto, o notifica a los TESORERO activos si tiene pagos vigentes (contracts/iasistencia.md)
- [X] T028 [US3] Crear `JAVA/rest/MultaController.java` con las rutas `/multas` de contracts/rutas-ui.md (lista, sincronizar GET/POST; los parámetros `monto[{asistenciaId}]` se leen del mapa de la petición)
- [X] T029 [US3] Crear `RES/templates/multas/lista.html` (multas con estado y saldo, botón "Sincronizar Multas de Asistencia" solo para TESORERO) y `RES/templates/multas/sincronizar.html` (contador "N inasistencias detectadas", tabla con check y monto editable, bloque de actas pendientes, botón "Aprobar y Facturar Multas")
- [X] T030 [US3] Quitar `/multas` de `JAVA/rest/EnConstruccionController.java` y ejecutar `TEST/MultaTest.java` y `TEST/CorreccionAsistenciaTest.java` hasta que pasen

**Checkpoint**: las inasistencias se convierten en deuda cobrable

---

## Phase 6: User Story 4 - Registrar egresos de la junta (Priority: P1) — CU11

**Goal**: el TESORERO registra salidas de dinero con comprobante y el saldo actual se recalcula.

**Independent Test**: `EgresoTest` en verde; paso 10 de quickstart.md.

### Tests for User Story 4 ⚠️

- [X] T031 [P] [US4] Escribir `TEST/EgresoTest.java` (CU11) con HU4-1 a HU4-7 de spec.md: lista con saldo actual, registro con factura PDF y saldo recalculado (pagos menos egresos), sin factura → "Pendiente de soporte" y adjunto posterior, monto mayor al saldo sin confirmar (no guarda) y confirmado (saldo negativo), categoría inválida, monto cero, concepto y fecha vacíos, adjunto de tipo no permitido, descarga del comprobante solo para la junta dueña, lectura para ADMIN_JUNTA con 403 al registrar o adjuntar, egreso de otra junta → 404; monto con tres decimales rechazado con `monto.decimales` (FR-037)

### Implementation for User Story 4

- [X] T032 [P] [US4] Crear `JAVA/model/Egreso.java` y `JAVA/repository/EgresoRepository.java` con data-model.md Egreso.junta, Egreso.categoria, Egreso.concepto, Egreso.monto, Egreso.fecha, Egreso.proveedor, Egreso.comprobanteNombre y Egreso.registradoPor; lista por junta (más recientes primero) y suma por junta
- [X] T033 [P] [US4] Crear `JAVA/service/DatosEgreso.java` y `JAVA/rest/formulario/EgresoFormulario.java` con validaciones de data-model.md Egreso.categoria, Egreso.concepto, Egreso.monto, Egreso.fecha y Egreso.proveedor
- [X] T034 [US4] Completar `JAVA/service/FinanzasService.java` `saldoActual` con la resta de egresos (FR-018) y crear `JAVA/service/EgresoService.java` (CU11): `registrar(datos, archivo, confirmarNegativo)` que exige confirmación si el monto supera el saldo, guarda el comprobante con `ArchivoService.validarDocumento` en data-model.md Egreso.comprobanteNombre, `adjuntar(id, archivo)` y `comprobante(id)`
- [X] T035 [US4] Crear `JAVA/rest/EgresoController.java` con las rutas `/egresos` de contracts/rutas-ui.md y agregar `/egresos` al manejo de archivo grande de `JAVA/rest/VistaComunAdvice.java`
- [X] T036 [US4] Crear `RES/templates/egresos/lista.html` (saldo actual, tabla responsive con "Pendiente de soporte" y adjuntar, modal "Nuevo Gasto" con los campos del mockup y aviso "Fondos insuficientes. ¿Forzar registro negativo?" con "Confirmar")
- [X] T037 [US4] Quitar `/egresos` de `JAVA/rest/EnConstruccionController.java` y ejecutar `TEST/EgresoTest.java` hasta que pase

**Checkpoint**: la junta conoce su saldo actual

---

## Phase 7: User Story 5 - Reversar un pago (Priority: P2) — CU31

**Goal**: el TESORERO anula un pago con un reverso exacto.

**Independent Test**: `ReversoTest` en verde; paso 9 de quickstart.md.

### Tests for User Story 5 ⚠️

- [X] T038 [P] [US5] Escribir `TEST/ReversoTest.java` (CU31) con HU5-1 a HU5-6 de spec.md: reverso con motivo y número propio correlativo con los pagos, obligaciones que vuelven exactamente a su saldo y estado anteriores (incluido un pago parcial intermedio), motivo vacío, segundo reverso rechazado, pago visible con "Reversado" y saldo actual recalculado, una cuota cuyo pago se reversó vuelve a poder anularse, ADMIN_JUNTA ve el reverso pero recibe 403 al reversar, pago de otra junta → 404

### Implementation for User Story 5

- [X] T039 [P] [US5] Crear `JAVA/model/ReversoPago.java` y `JAVA/repository/ReversoPagoRepository.java` con data-model.md ReversoPago.junta, ReversoPago.pago (único), ReversoPago.numero, ReversoPago.monto, ReversoPago.fecha, ReversoPago.motivo y ReversoPago.registradoPor; conteo por junta (para la numeración de T020) y búsqueda por pago
- [X] T040 [US5] Crear `JAVA/service/ReversoService.java` (CU31): `reversar(pagoId, motivo)` que exige data-model.md ReversoPago.motivo, rechaza si Pago.reversado, crea el reverso con número compartido (research.md R2), suma a cada Multa.saldoPendiente o Cuota.saldoPendiente lo aplicado y recalcula su estado (research.md R3); hacer que la numeración de `CobranzaService` cuente también los reversos
- [X] T041 [US5] Agregar a `JAVA/rest/CobranzaController.java` POST `/pagos/{id}/reversar` y a `RES/templates/pagos/detalle.html` y `RES/templates/pagos/historial.html` el botón "Reversar" (solo TESORERO y pago no reversado) con modal de motivo, la marca "Reversado", el motivo y el número del reverso
- [X] T042 [US5] Ejecutar `TEST/ReversoTest.java` y `TEST/CobranzaTest.java` hasta que pasen

**Checkpoint**: los pagos se corrigen sin borrarse

---

## Phase 8: User Story 6 - Gestionar inventario (Priority: P2) — CU13

**Goal**: el TESORERO lleva el stock con kardex y advertencias de stock bajo.

**Independent Test**: `InventarioTest` en verde; paso 11 de quickstart.md.

### Tests for User Story 6 ⚠️

- [X] T043 [P] [US6] Escribir `TEST/InventarioTest.java` (CU13) con HU6-1 a HU6-8 de spec.md: lista con estado OK/CRÍTICO, creación de ítem con código `INV-0001` y stock 0, nombre duplicado sin distinguir mayúsculas, stock mínimo negativo, entrada y salida con kardex (stock resultante, usuario), salida mayor al stock, cantidad cero, ítem en el mínimo marcado CRÍTICO sin bloquear, inactivar/reactivar, ítem inactivo rechazado en movimientos, eliminar ítem sin movimientos y rechazo con movimientos, lectura para ADMIN_JUNTA con 403 al modificar, ítem de otra junta → 404

### Implementation for User Story 6

- [X] T044 [P] [US6] Crear `JAVA/model/ItemInventario.java`, `JAVA/model/MovimientoInventario.java`, `JAVA/repository/ItemInventarioRepository.java` y `JAVA/repository/MovimientoInventarioRepository.java` con data-model.md ItemInventario.junta, ItemInventario.codigo, ItemInventario.nombre, ItemInventario.unidadMedida, ItemInventario.stock, ItemInventario.stockMinimo, ItemInventario.estado y MovimientoInventario.item, MovimientoInventario.tipo, MovimientoInventario.cantidad, MovimientoInventario.motivo, MovimientoInventario.fecha, MovimientoInventario.stockResultante, MovimientoInventario.usuario; conteo de ítems ACTIVOS con stock ≤ mínimo por junta
- [X] T045 [P] [US6] Crear `JAVA/service/DatosItem.java`, `JAVA/service/DatosMovimiento.java`, `JAVA/rest/formulario/ItemFormulario.java` y `JAVA/rest/formulario/MovimientoFormulario.java` con validaciones de data-model.md ItemInventario.nombre, ItemInventario.unidadMedida, ItemInventario.stockMinimo, MovimientoInventario.tipo, MovimientoInventario.cantidad y MovimientoInventario.motivo
- [X] T046 [US6] Crear `JAVA/service/InventarioService.java` (CU13): `crearItem` (código research.md R11, nombre único), `registrarMovimiento` (ítem activo, salida ≤ stock, kardex con stock resultante), `kardex(id)`, `cambiarEstado(id, activo)`, `eliminar(id)` (solo sin movimientos) y `itemsConStockBajo()`
- [X] T047 [US6] Crear `JAVA/rest/InventarioController.java` con las rutas `/inventario` de contracts/rutas-ui.md
- [X] T048 [US6] Crear `RES/templates/inventario/lista.html` (tabla del mockup con OK/CRÍTICO, modales "Nuevo Ítem" y "Nuevo Movimiento de Inventario" solo con ítems activos, acciones para TESORERO) y `RES/templates/inventario/kardex.html`
- [X] T049 [US6] Quitar `/inventario` de `JAVA/rest/EnConstruccionController.java` y ejecutar `TEST/InventarioTest.java` hasta que pase

**Checkpoint**: la bodega de la junta está controlada

---

## Phase 9: User Story 7 - Gestionar activos fijos (Priority: P2) — CU14

**Goal**: el TESORERO registra activos, ve su depreciación y el patrimonio, y los da de baja.

**Independent Test**: `ActivoFijoTest` en verde; paso 12 de quickstart.md.

### Tests for User Story 7 ⚠️

- [X] T050 [P] [US7] Escribir `TEST/ActivoFijoTest.java` (CU14) con HU7-1 a HU7-8 de spec.md: registro con código `AF-AAAA-0001`, depreciación de las cuatro categorías con fechas fijas (incluido tope en el valor de compra y meses incompletos), activo sin categoría de $200 o menos sin depreciación, más de $200 sin categoría rechazado, valor cero/negativo, presencia del script que deshabilita "Guardar Activo", fecha futura, archivo PDF/JPG/PNG y tipo inválido, patrimonio sin dados de baja, baja con motivo y rechazo sin motivo o repetida, lectura para ADMIN_JUNTA con 403, activo de otra junta → 404; prueba unitaria de `ActivoFijo.depreciacion(hoy)`; monto con tres decimales rechazado con `monto.decimales` (FR-037)

### Implementation for User Story 7

- [X] T051 [P] [US7] Crear `JAVA/model/ActivoFijo.java` y `JAVA/repository/ActivoFijoRepository.java` con data-model.md ActivoFijo.junta, ActivoFijo.codigo, ActivoFijo.nombre, ActivoFijo.categoria, ActivoFijo.fechaAdquisicion, ActivoFijo.valorAdquisicion, ActivoFijo.estado, ActivoFijo.archivoNombre, ActivoFijo.motivoBaja y ActivoFijo.fechaBaja, más `depreciacion(LocalDate hoy)` y `valorEnLibros(LocalDate hoy)` (research.md R7)
- [X] T052 [US7] Crear `JAVA/service/DatosActivo.java`, `JAVA/rest/formulario/ActivoFormulario.java`, `JAVA/service/ActivoFijoService.java` (CU14: `registrar`, `darDeBaja`, `listar` con patrimonio, `archivo`) y `JAVA/rest/ActivoFijoController.java` con las rutas `/activos` de contracts/rutas-ui.md; validar data-model.md ActivoFijo.valorAdquisicion, ActivoFijo.categoria (obligatoria sobre $200), ActivoFijo.fechaAdquisicion y ActivoFijo.motivoBaja; agregar `/activos` al manejo de archivo grande de `JAVA/rest/VistaComunAdvice.java` y quitar `/activos` de `JAVA/rest/EnConstruccionController.java`
- [X] T053 [US7] Crear `RES/templates/activos/lista.html` (tabla con depreciación, valor en libros y estado, total del patrimonio, modal "Registrar Activo" con los campos del mockup y script que deshabilita "Guardar Activo" mientras el valor no sea mayor que 0, modal "Dar de Baja")
- [X] T054 [US7] Ejecutar `TEST/ActivoFijoTest.java` hasta que pase

**Checkpoint**: el patrimonio de la junta está registrado

---

## Phase 10: User Story 8 - Emitir reportes financieros (Priority: P3) — CU12

**Goal**: el TESORERO y el ADMIN_JUNTA descargan el Balance General y Socios Morosos en PDF o
Excel.

**Independent Test**: `ReporteTest` en verde; paso 13 de quickstart.md.

### Tests for User Story 8 ⚠️

- [X] T055 [P] [US8] Escribir `TEST/ReporteTest.java` (CU12) con HU8-1 a HU8-7 de spec.md: formulario y vista previa con saldo del rango; Balance en PDF (leído con OpenPDF: nombre y RUC de la junta, rango, pago y su reverso negativo, egresos por categoría, saldo) y en Excel (leído con POI: mismas filas); Socios Morosos con cuota vencida y multa con saldo, incluida una cuota vencida antes de la fecha de inicio, sin anuladas, pagadas ni cuotas aún no vencidas; Balance desde la primera fecha hasta hoy cuyo saldo coincide con `FinanzasService.saldoActual()` (SC-004); rango vacío con "No existen registros en el rango seleccionado" sin archivo; fin anterior a inicio; descarga para ADMIN_JUNTA y TESORERO; 403 para SECRETARIO; datos de otra junta excluidos

### Implementation for User Story 8

- [X] T056 [P] [US8] Crear `JAVA/service/DatosReporte.java` y `JAVA/rest/formulario/ReporteFormulario.java` (tipo, inicio, fin, formato) y los enums `TipoReporte` y `FormatoReporte` de data-model.md en `JAVA/model/`
- [X] T057 [US8] Agregar a los repositorios de pagos, reversos, egresos, multas y cuotas las consultas por junta y rango de fechas que necesita el reporte; crear `JAVA/service/ReporteService.java` (CU12) que arma los records de Balance (ingresos, reversos negativos, egresos por categoría, saldo) y de Morosos (deuda vencida acumulada hasta la fecha de fin según FR-023: cuotas con vencimiento anterior a hoy y no posterior al fin, multas con fecha no posterior al fin, ambas con saldo y no anuladas; detalle y deuda total) y lanza `reporte.sinRegistros` si están vacíos (research.md R10)
- [X] T058 [P] [US8] Crear `JAVA/service/ReportePdf.java` (OpenPDF: logo de la junta si existe vía `ArchivoService`, nombre, RUC, tipo, rango, fecha de generación y tablas) y `JAVA/service/ReporteExcel.java` (POI: hoja con las mismas columnas)
- [X] T059 [US8] Crear `JAVA/rest/ReporteController.java` con las rutas `/reportes` de contracts/rutas-ui.md (`Content-Disposition: attachment` con el nombre de research.md R10) y `RES/templates/reportes/formulario.html` (tipo, fechas, formato PDF/Excel y vista previa del mockup); quitar `/reportes` de `JAVA/rest/EnConstruccionController.java`
- [X] T060 [US8] Ejecutar `TEST/ReporteTest.java` hasta que pase

**Checkpoint**: la directiva obtiene sus reportes

---

## Phase 11: Polish & Cross-Cutting Concerns

- [X] T061 Completar en `JAVA/service/PanelService.java` las tarjetas "Saldo actual", "Deuda por cobrar" (enlace `/pagos`) e "Ítems con stock bajo" con `FinanzasService` e `InventarioService` (FR-036) y ampliar `TEST/PanelTest.java` (valores reales solo de la junta, enlace nuevo y "Disponible próximamente" solo en "Mensajes no leídos")
- [X] T062 Ampliar `JAVA/config/DatosPrueba.java` según research.md R13 sin duplicar en reinicios, y actualizar `TEST/DatosPruebaTest.java` y `README.md` (sección 004)
- [X] T063 Prueba de rendimiento en `TEST/MultaTest.java`: aprobar 200 multas en menos de 3 s (SC-002); y en `TEST/ReporteTest.java`: reporte mensual con 500 movimientos en menos de 10 s (SC-006)
- [X] T064 Ejecutar `.\mvnw.cmd test` completo (001 a 004) y dejar todo en verde
- [X] T065 [P] Revisar que ningún archivo bajo `src/` tenga comentarios (Principio VI)
- [X] T066 [P] Revisar textos en español y ausencia de desplazamiento horizontal a 360 px en las pantallas nuevas (FR-038, SC-008), que todos los montos se muestren con "$" y dos decimales (FR-037), y comprobar que `JAVA/rest/EnConstruccionController.java` solo conserva `/convocatorias`, `/actas`, `/comunicaciones`, `/anuncios` y `/mensajes`
- [X] T067 Ejecutar la validación manual de `specs/004-gestion-financiera/quickstart.md` (pasos 1 a 16) y restaurar `data/` y `uploads/` al terminar
- [X] T068 Verificar trazabilidad: cada FR de spec.md cubierto por una tarea y una prueba con su CUxx (Principio VIII)

---

## Dependencies & Execution Order

- **Setup (1)** → **Foundational (2)** → historias
- **US1 (3)** CU30: tras Foundational
- **US2 (4)** CU09: tras Foundational (cobra multas y cuotas creadas con `Fabrica`); usa `Cuota` de Foundational
- **US3 (5)** CU10: tras Foundational; la prueba del efecto de CU28 con pago vigente usa `Fabrica.multa(socio, monto, saldo)`, sin depender de US2
- **US4 (6)** CU11: tras US2 (el saldo actual usa pagos)
- **US5 (7)** CU31: tras US2
- **US6 (8)** CU13 y **US7 (9)** CU14: tras Foundational, independientes del resto
- **US8 (10)** CU12: tras US2, US4 y US5 (reporta pagos, reversos y egresos) y US1/US3 (morosos)
- **Polish (11)**: tras todas; T061 necesita US2, US4 y US6

### Within Each User Story

- Prueba primero (debe fallar) → entidad y repositorio → formulario y datos → servicio →
  controlador y vistas → ejecución de pruebas

### Parallel Opportunities

- Setup: T002 y T003; Foundational: T004 a T008
- Tras Foundational, cuatro líneas en paralelo: US1 (cuotas), US2→US4/US5 (cobros, egresos y
  reversos), US3 (multas) y US6 + US7 (inventario y activos)
- Dentro de cada historia, las tareas [P] de prueba, entidad y formulario

---

## Parallel Example: User Story 2

```text
Task: "Escribir TEST/CobranzaTest.java (CU09)"
Task: "Crear JAVA/model/Pago.java, AplicacionPago.java y sus repositorios"
Task: "Crear JAVA/service/DatosCobro.java y JAVA/rest/formulario/CobroFormulario.java"
```

---

## Implementation Strategy

### MVP First

1. Setup + Foundational
2. US1 (CU30) → obligaciones emitidas
3. US2 (CU09) → cobro con recibo
4. US3 (CU10) → multas desde la asistencia

### Incremental Delivery

1. MVP (US1 a US3)
2. US4 → egresos y saldo actual
3. US5 → reversos
4. US6 y US7 → inventario y activos fijos
5. US8 → reportes PDF y Excel
6. Polish → panel, datos de prueba y puertas de calidad

---

## Notes

- [P] = archivos distintos, sin dependencias pendientes
- [USx] y el CUxx en la descripción dan la trazabilidad exigida por la constitución
- Detenerse en cada checkpoint para validar la historia

---

## Phase 12: Convergence

- [X] T069 Mostrar el botón "Cobrar" en todas las filas de Cobranzas, también para socios "Al día" (el cobro sin obligaciones muestra "El socio no tiene obligaciones pendientes"), en `RES/templates/pagos/cobranzas.html` y ajustar `TEST/CobranzaTest.java` per US2/AC1 (contradicts)
- [X] T070 Marcar como "Vencida" las cuotas con vencimiento anterior a hoy en la lista de Cobranzas y en el formulario de cobro (`RES/templates/pagos/cobranzas.html`, `RES/templates/pagos/cobro.html`) con prueba en `TEST/CobranzaTest.java` per spec.md Edge Cases (cuota vencida en Cobranzas) (partial)
