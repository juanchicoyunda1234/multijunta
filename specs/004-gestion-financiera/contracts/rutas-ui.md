# Contrato: rutas de la interfaz (004)

Las rutas de 001 a 003 siguen vigentes. Respuestas HTML en español; los POST llevan CSRF. Los
permisos salen de las filas ya existentes de `MatrizPermisos` (`/pagos`, `/multas`, `/egresos`,
`/reportes`, `/inventario`, `/activos`, `/cuotas`): los GET los ven los roles con acceso completo
(C) o de lectura (L); los POST solo los roles C. Un identificador de otra junta responde 404.

| Fila de la matriz | C | L |
|---|---|---|
| `/pagos` (CU09, CU31) | TESORERO | ADMIN_JUNTA |
| `/multas` (CU10) | TESORERO | ADMIN_JUNTA, SECRETARIO |
| `/egresos` (CU11) | TESORERO | ADMIN_JUNTA |
| `/reportes` (CU12) | ADMIN_JUNTA, TESORERO | — |
| `/inventario` (CU13) | TESORERO | ADMIN_JUNTA |
| `/activos` (CU14) | TESORERO | ADMIN_JUNTA |
| `/cuotas` (CU30) | TESORERO | ADMIN_JUNTA |

## Cuotas (CU30)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/cuotas?q=&estado=` | cuotas de la junta (socio, concepto, monto, vencimiento, saldo, estado) y botón "Emitir Cuotas" | CU30 |
| GET `/cuotas/emitir` | formulario "Emitir Cuotas" con los tres destinos | CU30 FR-001 |
| POST `/cuotas/previa` | valida y muestra "Se crearán N cuotas por un total de $X" y los omitidos, sin guardar | CU30 FR-002, FR-003 |
| POST `/cuotas` | `destino=TODOS\|SECTOR\|SOCIO`, `sectorId`, `socioId`, `concepto`, `monto`, `fechaVencimiento`; crea y vuelve a `/cuotas` con "Cuotas emitidas" | CU30 FR-001..003 |
| POST `/cuotas/{id}/anular` | `motivo`; anula si no tiene pagos | CU30 FR-004 |

## Pagos (CU09, CU31)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/pagos?q=` | Cobranzas: socios con deuda total o "Al día", botón "Cobrar" | CU09 FR-005 |
| GET `/pagos/cobrar/{socioId}` | obligaciones PENDIENTES y PARCIALES con check, total, monto y método | CU09 FR-006 |
| POST `/pagos/cobrar/{socioId}` | `obligaciones` (`M-{id}` / `C-{id}`), `monto`, `metodo`, `confirmarParcial`; si el monto es menor y no está confirmado, vuelve con el aviso de abono parcial; éxito → `/pagos/{id}` | CU09 FR-007, FR-008 |
| GET `/pagos/historial?q=` | pagos de la junta (número, fecha, socio, monto, método, "Reversado") | CU09, CU31 |
| GET `/pagos/{id}` | detalle: aplicaciones, estado de cuenta del socio ("Al día"), botones "Imprimir Recibo" y "Reversar" (solo TESORERO y si no está reversado) | CU09 FR-009, CU31 |
| GET `/pagos/{id}/recibo` | recibo imprimible con logo, nombre y RUC | CU09 FR-009 |
| POST `/pagos/{id}/reversar` | `motivo`, `confirmar`; crea el reverso → `/pagos/{id}` con "Pago reversado" | CU31 FR-019..021 |

## Multas (CU10)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/multas` | multas generadas (socio, motivo, monto, saldo, estado) y botón "Sincronizar Multas de Asistencia" | CU10 |
| GET `/multas/sincronizar` | tabla de inasistencias sin multa con check y monto editable + bloque de eventos sin acta cerrada | CU10 FR-011, FR-014 |
| POST `/multas/sincronizar` | `seleccion` (ids de asistencia) y `monto[{asistenciaId}]`; crea las multas → `/multas` con "Se generaron N multas por $X" | CU10 FR-012, FR-013 |

## Egresos (CU11)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/egresos` | lista de egresos, "Saldo Actual" y botón "Nuevo Gasto" | CU11 |
| POST `/egresos` (multipart) | `categoria`, `concepto`, `monto`, `fecha`, `proveedor`, `comprobante`, `confirmarNegativo`; si supera el saldo y no está confirmado, vuelve con "Fondos insuficientes. ¿Forzar registro negativo?" | CU11 FR-016..018 |
| POST `/egresos/{id}/comprobante` (multipart) | adjunta la factura a un egreso pendiente de soporte | CU11 FR-017 |
| GET `/egresos/{id}/comprobante` | sirve el archivo; 404 si no hay | CU11 |

## Reportes (CU12)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/reportes?tipo=&inicio=&fin=` | formulario y vista previa con el saldo del rango | CU12 |
| POST `/reportes` | `tipo=BALANCE\|MOROSOS`, `inicio`, `fin`, `formato=PDF\|EXCEL`; descarga `balance-AAAAMMDD-AAAAMMDD.pdf` / `.xlsx` o vuelve con "No existen registros en el rango seleccionado" | CU12 FR-022..025 |

## Inventario (CU13)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/inventario` | ítems con stock y estado OK/CRÍTICO, botones "Nuevo Ítem" y "Nuevo Movimiento de Inventario" | CU13 |
| POST `/inventario/items` | `nombre`, `unidadMedida`, `stockMinimo` | CU13 FR-026 |
| POST `/inventario/movimientos` | `itemId`, `tipo`, `cantidad`, `motivo` | CU13 FR-027, FR-028 |
| GET `/inventario/items/{id}` | kardex del ítem | CU13 FR-027 |
| POST `/inventario/items/{id}/estado` | `activo=true\|false` | CU13 FR-029 |
| POST `/inventario/items/{id}/eliminar` | elimina si no tiene movimientos | CU13 FR-029 |

## Activos fijos (CU14)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/activos` | activos con depreciación y valor en libros, total del patrimonio, botón "Registrar Activo" | CU14 FR-032 |
| POST `/activos` (multipart) | `nombre`, `categoria`, `fechaAdquisicion`, `valorAdquisicion`, `estado`, `archivo` | CU14 FR-030, FR-031 |
| POST `/activos/{id}/baja` | `motivo` | CU14 FR-033 |
| GET `/activos/{id}/archivo` | sirve la foto o el acta; 404 si no hay | CU14 |

## Panel (CU25)

Las tarjetas "Saldo actual" (enlace `/reportes`), "Deuda por cobrar" (enlace `/pagos`) e "Ítems
con stock bajo" (enlace `/inventario`) muestran valores reales (FR-036).
