# Quickstart: validar el Módulo Gestión Financiera

## Preparación

1. Borrar `./data` y `./uploads` para partir de los datos de prueba (research R13).
2. Ejecutar las pruebas y arrancar la aplicación:

```powershell
.\mvnw.cmd test
```

```powershell
.\mvnw.cmd spring-boot:run
```

3. Abrir `http://localhost:8080`. Contraseña de todos: `Multijunta2026` (README).

## Escenarios (junta San Pedro)

| # | Usuario | Pasos | Resultado esperado |
|---|---|---|---|
| 1 | `tesorero.sanpedro` | Cuotas → Emitir Cuotas → "Consumo de medidor", $3.50, vencimiento en 15 días, Todos los socios activos → Vista previa → Confirmar | Vista previa "Se crearán N cuotas por un total de $X"; luego "Cuotas emitidas" (CU30) |
| 2 | tesorero | Repetir el paso 1 con el mismo concepto | Todos se omiten: "No hay socios a quienes emitir esta cuota" (FR-003) |
| 3 | tesorero | Anular una de las cuotas nuevas con motivo; intentar anular la "Tarifa anual 2026" que tiene pago parcial | La primera queda ANULADA; la segunda no ofrece "Anular" (FR-004) |
| 4 | tesorero | Multas → Sincronizar Multas de Asistencia | Aparece el ausente de la Minga cerrada con $5.00; el justificado no aparece; la Asamblea de ayer sale bloqueada con "Acta de evento no oficializada por Secretaría" (CU10) |
| 5 | tesorero | Cambiar el monto a $6.00 y Aprobar y Facturar Multas; volver a sincronizar | "Se generaron 1 multas por $6.00"; la segunda sincronización dice "No hay inasistencias pendientes de multar" (FR-013) |
| 6 | tesorero | Pagos → Cobrar al socio multado → dejar marcadas la multa y la cuota, monto igual al total, Efectivo → Registrar Pago | Número `TX-…`, obligaciones PAGADAS, "Al día" en verde; Imprimir Recibo muestra logo, nombre y RUC (CU09) |
| 7 | tesorero | Cobrar a otro socio con un monto menor al total | Aviso de abono parcial; al confirmar, la obligación más antigua queda PAGADA y la siguiente PARCIAL (FR-007) |
| 8 | tesorero | Cobrar con monto 0 y con monto mayor al total | Rechazo "El monto debe ser mayor que cero y no superar el total seleccionado" |
| 9 | tesorero | Abrir el pago del paso 6 → Reversar con motivo; intentar reversar otra vez | "Pago reversado"; multa y cuota vuelven a PENDIENTE; el pago muestra "Reversado" y no ofrece el botón (CU31) |
| 10 | tesorero | Egresos → Nuevo Gasto con factura PDF; otro sin factura y adjuntarla luego; otro mayor al saldo | Saldo recalculado; "Pendiente de soporte" desaparece al adjuntar; aviso "Fondos insuficientes…" y saldo negativo al confirmar (CU11) |
| 11 | tesorero | Inventario → entrada y salida de un ítem; salida mayor al stock; dejar un ítem en el mínimo; inactivar un ítem con movimientos | Kardex actualizado; "Stock insuficiente para esta salida"; fila "CRÍTICO"; ítem inactivo fuera de la lista de movimientos (CU13) |
| 12 | tesorero | Activos → Registrar Activo (Vehículos, $12 000, comprado hace 30 meses); intentar $0; uno de $300 sin categoría; dar de baja otro | Depreciación $6 000 y valor en libros $6 000; guardado deshabilitado con $0; rechazo de categoría; patrimonio sin el activo dado de baja (CU14) |
| 13 | tesorero | Reportes → Balance General del mes en PDF y en Excel; Socios Morosos; un rango sin movimientos | Archivos descargados con logo y RUC (PDF); el pago y su reverso aparecen; "No existen registros en el rango seleccionado" (CU12) |
| 14 | `admin.sanpedro` | Aprobar una corrección de AUSENTE a JUSTIFICADO para un socio con multa sin pagos (pedida antes por `secretaria.sanpedro`) | La multa queda ANULADA con motivo "Asistencia corregida"; con una multa pagada, el tesorero recibe la notificación (FR-015) |
| 15 | `admin.sanpedro` y `secretaria.sanpedro` | Abrir Pagos, Multas, Egresos, Inventario, Activos y Cuotas; intentar un POST | El admin ve todo sin botones de acción y recibe 403; la secretaria solo ve Multas; ambos ven las tarjetas del panel con valores reales (FR-034, FR-036) |
| 16 | tesorero | Recorrer todas las pantallas a 360 px | Sin desplazamiento horizontal (FR-038) |

Al terminar, borrar `./data` y `./uploads` para dejar los datos de prueba limpios.
