# Data Model: Módulo Gestión Administrativa

Base: `docs/modelo-dominio.md`, `specs/001-base-acceso-sesion/data-model.md` y
`specs/002-gestion-plataforma/data-model.md`. Todas las entidades nuevas tienen `Long id`
autogenerado. Solo se repiten reglas de entidades existentes cuando esta funcionalidad las
amplía.

## Enums nuevos

| Enum | Valores |
|---|---|
| TipoEvento | ASAMBLEA, MINGA, REUNION |
| EstadoEvento | PROGRAMADO, CANCELADO |
| EstadoAsistencia | PRESENTE, AUSENTE, JUSTIFICADO |
| EstadoCorreccion | PENDIENTE, APROBADA, RECHAZADA |
| TipoRecurso | COMPUERTA, CANAL, RESERVORIO |

## Socio (existente) — reglas de CU05

| Campo | Regla en esta funcionalidad |
|---|---|
| cedula | 10 dígitos, única en la plataforma (002); no editable después de registrar |
| nombres, apellidos | obligatorios, hasta 100 |
| celular | opcional, exactamente 10 dígitos si se ingresa |
| sector | obligatorio, de la junta del secretario |
| codigoSocio | generado `S-0001` correlativo por junta, único en la junta |
| fechaIngreso | fecha del registro |
| estado | ACTIVO al registrar; editable ACTIVO/INACTIVO |
| tieneCuenta, activo | `false` al registrar (la cuenta se crea en CU04) |
| username | igual a la cédula |
| password | al registrar sin cuenta: hash BCrypt de un valor aleatorio que nadie conoce (la columna es obligatoria desde 001); CU04 lo reemplaza al crear la cuenta |

## Evento (nuevo)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio, la del usuario |
| tipo | TipoEvento | obligatorio |
| fecha | LocalDate | obligatoria |
| hora | LocalTime | obligatoria |
| lugar | String(150) | obligatorio |
| descripcion | String(500) | opcional |
| estado | EstadoEvento | PROGRAMADO al crear |
| asistenciaCerrada | boolean | `false` al crear |

Transiciones: `PROGRAMADO --Cancelar (sin asistencia)--> CANCELADO`;
`asistenciaCerrada false --Cerrar registro--> true` (irreversible). Editar o cancelar solo si
`asistenciaCerrada = false`. Toma de asistencia solo si tipo ASAMBLEA o MINGA, PROGRAMADO,
`fecha <= hoy` y `asistenciaCerrada = false`.

## Asistencia (nueva)

| Campo | Tipo | Reglas |
|---|---|---|
| evento | Evento | obligatorio; la junta se toma del evento |
| socio | Socio | obligatorio, de la misma junta |
| fecha | LocalDate | la del evento |
| estado | EstadoAsistencia | obligatorio |
| observacion | String(300) | opcional |

Única por (evento, socio). Solo cambia por una CorreccionAsistencia aprobada.

## CorreccionAsistencia (nueva)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | la del evento |
| asistencia | Asistencia | de un evento con asistencia cerrada |
| estadoAnterior | EstadoAsistencia | el estado al solicitar |
| estadoNuevo | EstadoAsistencia | distinto de estadoAnterior |
| motivo | String(300) | obligatorio |
| estado | EstadoCorreccion | PENDIENTE al crear |
| fechaSolicitud | LocalDateTime | al crear |
| fechaResolucion | LocalDateTime | al aprobar o rechazar |
| solicitante | Usuario | SECRETARIO de la junta |
| resolutor | Usuario | ADMIN_JUNTA de la junta |

Transiciones: `PENDIENTE --Aprobar--> APROBADA` (cambia Asistencia.estado);
`PENDIENTE --Rechazar--> RECHAZADA`. Como máximo una PENDIENTE por asistencia. Ningún campo se
edita después de resuelta.

## Predio (nuevo)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | la del socio |
| socio | Socio | obligatorio |
| sector | Sector | obligatorio, de la junta |
| codigo | String(20) | generado `P-0001` correlativo por junta |
| area | BigDecimal(12,2) | obligatoria, mayor que 0 (m²) |
| ubicacion | String(200) | referencia opcional (texto) |
| latitud | BigDecimal(9,6) | obligatoria, entre −5.1 y 2.0 |
| longitud | BigDecimal(9,6) | obligatoria, entre −92.1 y −75.1 |
| fotoNombre | String(100) | opcional; archivo PNG/JPG de hasta 5 MB en `./uploads/predios` |

## Recurso (nuevo)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| nombre | String(80) | obligatorio, único en la junta sin distinguir mayúsculas |
| tipo | TipoRecurso | obligatorio |
| sector | Sector | obligatorio, de la junta |
| caudal | BigDecimal(10,2) | obligatorio, mayor que 0 (L/s) |
| activo | boolean | `true` al crear |

## Turno (nuevo)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| fecha | LocalDate | obligatoria, de lunes a viernes |
| horaInicio | LocalTime | obligatoria |
| horaFin | LocalTime | obligatoria, posterior a horaInicio |
| responsable | String(100) | obligatorio |
| sector | Sector | obligatorio, de la junta |
| recurso | Recurso | obligatorio, activo al crear o reprogramar |

Regla: no existen dos turnos del mismo recurso en la misma fecha con `inicio < finOtro` y
`fin > inicioOtro`.

## Sector (existente) — regla ampliada

No se quita si tiene socios, predios, recursos o turnos asignados.

## Inasistencia (nuevo, no persistido)

`Inasistencia(eventoId, tipoEvento, fecha, socioId, cedula, nombreCompleto, asistenciaId)`; ver
`contracts/iasistencia.md`.
