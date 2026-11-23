# Contrato: mensajes exactos (004)

Se reutilizan `campo.obligatorio` ("Este campo es obligatorio") y el texto "El archivo excede el
límite permitido" de 003.

| Clave | Texto | CU |
|---|---|---|
| monto.positivo | El monto debe ser mayor que cero | CU09, CU10, CU11, CU30 |
| monto.decimales | El monto admite hasta dos decimales | todos |
| cuota.previa | Se crearán {0} cuotas por un total de ${1} | CU30 |
| cuota.omitidos | {0} socios ya tienen esta cuota y se omitirán | CU30 |
| cuota.ninguna | No hay socios a quienes emitir esta cuota | CU30 |
| cuota.emitidas | Cuotas emitidas | CU30 |
| cuota.duplicada | Este socio ya tiene una cuota con ese concepto | CU30 |
| cuota.anulada | Cuota anulada | CU30 |
| cuota.conPagos | Una cuota con pagos no se puede anular | CU30 |
| motivo.obligatorio | El motivo es obligatorio | CU30, CU31, CU14 |
| pago.seleccion | Seleccione al menos una obligación | CU09 |
| pago.montoInvalido | El monto debe ser mayor que cero y no superar el total seleccionado | CU09 |
| pago.confirmarParcial | El monto es menor al total. ¿Registrar como abono parcial? | CU09 |
| pago.registrado | Pago registrado | CU09 |
| pago.alDia | Al día | CU09 |
| pago.cambio | Las obligaciones cambiaron; vuelva a cargar el cobro | CU09 |
| pago.reversado | Pago reversado | CU31 |
| pago.yaReversado | Este pago ya fue reversado | CU31 |
| multa.sinPendientes | No hay inasistencias pendientes de multar | CU10 |
| multa.actaPendiente | Acta de evento no oficializada por Secretaría | CU10 |
| multa.generadas | Se generaron {0} multas por ${1} | CU10 |
| multa.seleccion | Seleccione al menos una inasistencia | CU10 |
| multa.duplicada | Esta inasistencia ya tiene una multa | CU10 |
| multa.motivo | Inasistencia a {0} del {1} | CU10 |
| multa.anuladaCorreccion | Asistencia corregida | CU28 |
| notificacion.multaPagadaCorregida.titulo | Multa pagada en asistencia corregida | CU28, CU31 |
| egreso.registrado | Egreso registrado | CU11 |
| egreso.fondos | Fondos insuficientes. ¿Forzar registro negativo? | CU11 |
| egreso.pendienteSoporte | Pendiente de soporte | CU11 |
| egreso.soporteAdjuntado | Comprobante adjuntado | CU11 |
| archivo.documentoTipo | Formato no permitido. Use PDF, JPG o PNG | CU11, CU14 |
| archivo.grande | El archivo excede el límite permitido | CU11, CU14 |
| reporte.sinRegistros | No existen registros en el rango seleccionado | CU12 |
| reporte.rango | La fecha de fin debe ser igual o posterior a la de inicio | CU12 |
| inventario.itemCreado | Ítem creado | CU13 |
| inventario.duplicado | Ya existe un ítem con ese nombre | CU13 |
| inventario.movimiento | Movimiento registrado | CU13 |
| inventario.stockInsuficiente | Stock insuficiente para esta salida | CU13 |
| inventario.cantidad | La cantidad debe ser mayor que cero | CU13 |
| inventario.stockMinimo | El stock mínimo no puede ser negativo | CU13 |
| inventario.inactivo | El ítem está inactivo | CU13 |
| inventario.estado | Estado del ítem actualizado | CU13 |
| inventario.eliminado | Ítem eliminado | CU13 |
| inventario.conMovimientos | Un ítem con movimientos no se puede eliminar; inactívelo | CU13 |
| activo.registrado | Activo registrado | CU14 |
| activo.valor | El valor de compra debe ser mayor que cero | CU14 |
| activo.categoria | Los activos de más de $200 deben tener categoría | CU14 |
| activo.fechaFutura | La fecha de adquisición no puede ser futura | CU14 |
| activo.baja | Activo dado de baja | CU14 |
| activo.yaBaja | El activo ya está dado de baja | CU14 |
