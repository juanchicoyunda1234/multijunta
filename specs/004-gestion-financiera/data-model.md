# Data Model: Módulo Gestión Financiera

Todas las entidades llevan `Long id` autogenerado y `junta` (ManyToOne obligatorio) salvo las
que la heredan de su padre. Montos `BigDecimal(12,2)`, fechas `LocalDate`, instantes
`LocalDateTime`. Tablas creadas por JPA (`ddl-auto=update`).

## Enums

| Enum | Valores (etiqueta) |
|---|---|
| `EstadoObligacion` | PENDIENTE (Pendiente), PARCIAL (Parcial), PAGADA (Pagada), ANULADA (Anulada) |
| `MetodoPago` | EFECTIVO (Efectivo), TRANSFERENCIA (Transferencia) |
| `CategoriaEgreso` | MATERIALES (Materiales), HONORARIOS (Honorarios), SERVICIOS_BASICOS (Servicios básicos), MANTENIMIENTO (Mantenimiento), OTROS (Otros) |
| `EstadoItem` | ACTIVO (Activo), INACTIVO (Inactivo) |
| `TipoMovimiento` | ENTRADA (Entrada), SALIDA (Salida) |
| `CategoriaActivo` | INFRAESTRUCTURA (Infraestructura, 20 años), MAQUINARIA (Maquinaria, 10), EQUIPO_OFICINA (Equipo de oficina, 10), VEHICULOS (Vehículos, 5) |
| `EstadoActivo` | NUEVO (Nuevo), USADO (Usado), DADO_DE_BAJA (Dado de baja) |
| `TipoReporte` | BALANCE (Balance General de Ingresos y Egresos), MOROSOS (Socios Morosos) |
| `FormatoReporte` | PDF, EXCEL |

## Entidades

### Cuota (CU30)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| socio | Socio | obligatorio, de la junta |
| concepto | String(120) | obligatorio, recortado |
| conceptoNormalizado | String(120) | minúsculas y sin espacios extremos; único con `socio` entre cuotas no ANULADAS (validado en servicio) |
| monto | BigDecimal | > 0, 2 decimales |
| fecha | LocalDate | emisión = hoy |
| fechaVencimiento | LocalDate | obligatoria |
| estado | EstadoObligacion | nace PENDIENTE |
| saldoPendiente | BigDecimal | nace = monto; 0 ≤ saldo ≤ monto |
| motivoAnulacion | String(255) | obligatorio al anular |
| version | Long | `@Version` (R4) |

Transiciones: PENDIENTE → PARCIAL/PAGADA (pago); PARCIAL → PAGADA (pago); PARCIAL/PAGADA →
PENDIENTE/PARCIAL (reverso); PENDIENTE con `saldoPendiente = monto` (sin pagos vigentes, research R3) → ANULADA (anular).

### Multa (CU10)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| socio | Socio | obligatorio |
| asistenciaId | Long | único (FR-013); viene de `IAsistencia` |
| eventoId, tipoEvento, fechaEvento | Long, TipoEvento, LocalDate | copiados de la inasistencia |
| motivo | String(160) | "Inasistencia a Minga del 12/11/2026" |
| monto | BigDecimal | > 0 |
| fecha | LocalDate | día de la aprobación |
| estado | EstadoObligacion | nace PENDIENTE |
| saldoPendiente | BigDecimal | nace = monto |
| motivoAnulacion | String(255) | "Asistencia corregida" al anular por CU28 |
| version | Long | `@Version` |

Transiciones: como `Cuota`; PENDIENTE con `saldoPendiente = monto` → ANULADA solo por
`AsistenciaCorregida` (R6).

### Pago (CU09)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| socio | Socio | obligatorio |
| numero | String(12) | `TX-000001`, único por junta (R2) |
| monto | BigDecimal | > 0 y ≤ total marcado |
| fecha | LocalDateTime | instante del registro |
| concepto | String(255) | resumen de lo pagado ("Tarifa anual 2026, Multa Minga 12/11/2026") |
| metodo | MetodoPago | obligatorio |
| registradoPor | Usuario | tesorero |
| reversado | boolean | nace false |
| aplicaciones | List\<AplicacionPago\> | 1..* |

Sin edición ni borrado (FR-010).

### AplicacionPago (CU09, CU31)

| Campo | Tipo | Reglas |
|---|---|---|
| pago | Pago | obligatorio |
| multa | Multa | exactamente uno de `multa` o `cuota` |
| cuota | Cuota | |
| monto | BigDecimal | > 0 |

### ReversoPago (CU31)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| pago | Pago | OneToOne, único (FR-021) |
| numero | String(12) | misma numeración que pagos |
| monto | BigDecimal | = monto del pago |
| fecha | LocalDateTime | |
| motivo | String(255) | obligatorio |
| registradoPor | Usuario | tesorero |

### Egreso (CU11)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| categoria | CategoriaEgreso | obligatoria |
| concepto | String(160) | obligatorio |
| monto | BigDecimal | > 0 |
| fecha | LocalDate | obligatoria |
| proveedor | String(120) | opcional |
| comprobanteNombre | String | archivo en `uploads/egresos`; `null` = "Pendiente de soporte" |
| registradoPor | Usuario | tesorero |

### ItemInventario (CU13)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| codigo | String(10) | `INV-0001` |
| nombre | String(120) | único en la junta sin distinguir mayúsculas |
| unidadMedida | String(30) | obligatoria |
| stock | BigDecimal(12,2) | nace 0, nunca < 0 |
| stockMinimo | BigDecimal(12,2) | ≥ 0 |
| estado | EstadoItem | nace ACTIVO |

Derivado: `critico = stock ≤ stockMinimo`.

### MovimientoInventario (CU13)

| Campo | Tipo | Reglas |
|---|---|---|
| item | ItemInventario | obligatorio |
| tipo | TipoMovimiento | obligatorio |
| cantidad | BigDecimal | > 0; SALIDA ≤ stock |
| motivo | String(160) | obligatorio |
| fecha | LocalDateTime | instante del registro |
| stockResultante | BigDecimal | stock después del movimiento |
| usuario | Usuario | tesorero |

### ActivoFijo (CU14)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| codigo | String(14) | `AF-2026-0001` |
| nombre | String(120) | obligatorio |
| categoria | CategoriaActivo | obligatoria si valor > 200 |
| fechaAdquisicion | LocalDate | no futura |
| valorAdquisicion | BigDecimal | > 0 |
| estado | EstadoActivo | NUEVO o USADO al registrar |
| archivoNombre | String | `uploads/activos`, opcional |
| motivoBaja | String(255) | obligatorio al dar de baja |
| fechaBaja | LocalDate | día de la baja |

Derivados: `depreciacion(hoy) = min(valor, valor × mesesCompletos / (vidaUtil × 12))` (0 sin
categoría); `valorEnLibros = valor − depreciacion`.

## Cambios a entidades existentes

- Ninguna columna nueva en entidades de 001–003.
- `IAsistencia` (003) agrega `List<EventoSinCerrar> eventosSinCerrar(Long juntaId)` y el record
  `EventoSinCerrar(Long eventoId, TipoEvento tipoEvento, LocalDate fecha, String lugar)`.
- Nuevo record `AsistenciaCorregida(Long juntaId, Long asistenciaId, EstadoAsistencia
  estadoAnterior, EstadoAsistencia estadoNuevo)` publicado por `CorreccionAsistenciaService` al
  aprobar.

## Consultas agregadas (R7)

| Valor | Definición |
|---|---|
| Saldo actual | Σ `Pago.monto` con `reversado = false` − Σ `Egreso.monto` |
| Deuda del socio | Σ `saldoPendiente` de `Multa` y `Cuota` del socio con estado ≠ ANULADA |
| Deuda por cobrar (panel) | lo mismo para toda la junta |
| Ítems con stock bajo (panel) | count de `ItemInventario` ACTIVO con `stock ≤ stockMinimo` |
| Patrimonio | Σ `valorEnLibros` de activos con estado ≠ DADO_DE_BAJA |
