# Módulo 3: Gestión Financiera

Mockup: `docs/mockups/modulo3-CU09-CU14.png`.

## CU09 Registrar ingresos por pagos
- Actor: Tesorero/a. Prioridad alta.
- Disparador: botón "Cobrar" junto a un socio en el módulo de Cobranzas.
- Flujo: el sistema lista las obligaciones pendientes del socio (multas y cuotas) con checks → el tesorero marca las que paga y elige el método (Efectivo, Transferencia) → clic en "Registrar Pago" → el sistema actualiza el estado de cuenta (verde "Al día" si no queda deuda), genera un número de transacción y ofrece "Imprimir Recibo".
- Alterno: si el monto es menor al total seleccionado, pide confirmar un abono parcial y deja el saldo restante pendiente.
- Excepción: monto cero, negativo o mayor al total seleccionado se rechaza.
- Reglas: un pago no se borra; solo se anula con una transacción de reverso. El tesorero también registra cuotas (tarifa anual, medidor) a los socios.

## CU10 Generar multas (incluye CU06)
- Actores: Tesorero/a (primario), Secretario/a (secundario). Prioridad media.
- Disparador: botón "Sincronizar Multas de Asistencia" en Tesorería.
- Precondición: existe un registro de asistencia cerrado.
- Flujo: el sistema muestra una tabla de eventos recientes contra socios AUSENTES con el monto estándar de la junta → el tesorero puede editar montos → clic en "Aprobar y Facturar Multas" → el sistema crea las multas en lote y aumenta la deuda de cada socio.
- Alterno: desmarcar a un socio para excluirlo aunque figure como Ausente.
- Excepción: si el registro del evento no está cerrado, la tabla aparece bloqueada con "Acta de evento no oficializada por Secretaría".
- Reglas: los JUSTIFICADOS no aparecen; una misma asistencia no genera dos multas.

## CU11 Registrar egresos de la junta
- Actor: Tesorero/a. Prioridad alta.
- Disparador: botón "Nuevo Gasto" en la pestaña Egresos.
- Flujo: formulario con Concepto (categoría predefinida), Monto, Fecha y Proveedor, y adjunto PDF opcional de la factura → clic en "Registrar Salida de Dinero" → el sistema agrega el movimiento y recalcula "Saldo Actual" (ingresos menos egresos).
- Alterno: sin factura, el egreso queda "Pendiente de soporte" y se adjunta después.
- Excepción: si el monto supera el Saldo Actual, pide confirmar "Fondos insuficientes. ¿Forzar registro negativo?".
- Reglas: los egresos usan solo categorías predefinidas.

## CU12 Gestionar comprobantes y emitir reportes
- Actor: Tesorero/a. Prioridad media.
- Disparador: botón "Exportar / Reportes".
- Flujo: elige Tipo de Reporte (Balance General, Socios Morosos), fecha inicio y fin, y formato (PDF o Excel) → clic en "Generar Reporte" → el sistema genera y descarga el archivo.
- Excepción: si no hay movimientos en el rango, muestra "No existen registros en el rango seleccionado" en vez de un archivo vacío.
- Reglas: los PDF llevan el logo y el RUC de la junta.

## CU13 Gestionar inventarios
- Actor: Tesorero/a. Prioridad media.
- Disparador: botón "Nuevo Movimiento de Inventario".
- Flujo: el sistema lista los ítems con su stock → elige un ítem o crea uno nuevo (nombre, unidad de medida, stock mínimo) → indica Entrada o Salida, cantidad y motivo (ej. "Compra", "Uso en minga") → el sistema actualiza el stock, registra el movimiento en el kardex y confirma.
- Alterno: si el stock queda en o bajo el mínimo, la fila se resalta como advertencia sin bloquear.
- Excepción: una salida mayor al stock se rechaza con "Stock insuficiente para esta salida".
- Reglas: un ítem con movimientos no se elimina, solo se marca INACTIVO.

## CU14 Gestionar activos fijos
- Actor: Tesorero/a. Prioridad media.
- Disparador: botón "Registrar Activo".
- Flujo: ficha con nombre, categoría, fecha de adquisición, valor de compra, estado (Nuevo, Usado) y foto opcional → clic en "Guardar Activo" → el sistema calcula la depreciación acumulada según la categoría y lo agrega al patrimonio.
- Alterno: "Dar de Baja" con motivo; el activo pasa a DADO_DE_BAJA, sale del patrimonio activo y conserva su historial.
- Excepción: valor de compra cero o negativo deshabilita el guardado.
- Reglas: todo activo de más de $200 debe tener categoría. Depreciación lineal por vida útil de la categoría: Infraestructura 20 años, Maquinaria 10, Equipo de oficina 10, Vehículos 5.

---
# Casos de uso agregados en la fase de especificación

## CU30 Registrar cuotas y obligaciones
- Actor: Tesorero/a. Prioridad alta.
- Disparador: botón "Emitir Cuotas" en el módulo de Cobranzas.
- Flujo: el tesorero ingresa concepto (ej. "Tarifa anual 2026", "Consumo de medidor"), monto, fecha de vencimiento y a quién se aplica (todos los socios activos, un sector o un socio) → el sistema muestra cuántas cuotas va a crear y el total → confirma → crea las cuotas PENDIENTES que luego se cobran en el CU09.
- Alterno: anular una cuota sin pagos con un motivo.
- Excepciones: monto cero o negativo; un mismo concepto no se emite dos veces al mismo socio.
- Reglas: una cuota con pagos no se anula; las cuotas vencidas aparecen en el reporte de Socios Morosos (CU12).

## CU31 Reversar un pago
- Actor: Tesorero/a. Prioridad media.
- Disparador: botón "Reversar" en el detalle de un pago.
- Flujo: el sistema pide el motivo obligatorio y confirmación → crea una transacción de reverso por el mismo monto, marca el pago como reversado y devuelve el saldo a las multas o cuotas que ese pago había cancelado → confirma.
- Excepción: un pago ya reversado no se reversa otra vez.
- Reglas: el pago original no se borra; los reportes muestran el pago y su reverso. Solo el tesorero puede reversar.
