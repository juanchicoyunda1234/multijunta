# Data Model: Base del sistema, acceso por roles y gestión de sesión

Base: `docs/modelo-dominio.md`. Todas las entidades tienen `Long id` autogenerado. Se crean
aquí solo las entidades que esta funcionalidad necesita; las demás llegan con sus CU.

## Enums

| Enum | Valores |
|---|---|
| Rol | SUPER_ADMIN, ADMIN_JUNTA, SECRETARIO, TESORERO, SOCIO |
| TipoJunta | RIEGO, PARROQUIAL, COMUNITARIA |
| EstadoSocio | ACTIVO, INACTIVO |
| EstadoSolicitud | PENDIENTE, ATENDIDA |

## Junta

| Campo | Tipo | Reglas |
|---|---|---|
| nombre | String(150) | obligatorio |
| tipo | TipoJunta | obligatorio |
| ruc | String(13) | obligatorio, exactamente 13 dígitos, único |
| direccion | String(200) | opcional en esta funcionalidad (obligatorio en CU03) |
| telefono | String(20) | opcional |
| provincia | String(60) | obligatorio |
| canton | String(60) | obligatorio |
| fechaRegistro | LocalDate | se asigna al crear |
| logoUrl | String | opcional |
| montoMultaInasistencia | BigDecimal(10,2) | opcional, ≥ 0 |
| activa | boolean | por defecto `true` |
| motivoSuspension | String(300) | solo si `activa = false` |

Relaciones: Junta 1 → 0..* Usuario; Junta 1 → 0..* Sector.

## Usuario

| Campo | Tipo | Reglas |
|---|---|---|
| nombres | String(100) | obligatorio |
| apellidos | String(100) | obligatorio |
| cedula | String(10) | obligatorio, 10 dígitos, único en toda la plataforma (una persona, una cuenta) |
| email | String(120) | opcional, formato correo |
| username | String(50) | obligatorio, único global, se guarda en minúsculas y sin espacios |
| password | String(60) | hash BCrypt, nunca texto plano (FR-021) |
| rol | Rol | obligatorio, uno solo (FR-011) |
| junta | Junta | obligatorio salvo `rol = SUPER_ADMIN` (FR-012) |
| activo | boolean | una cuenta inactiva no inicia sesión |
| debeCambiarPassword | boolean | por defecto `true` en cuentas nuevas |
| intentosFallidos | int | 0..4; se reinicia al ingresar o al bloquear |
| bloqueadoHasta | LocalDateTime | nulo si no está bloqueado |

Transiciones de acceso (CU20):

```text
intentosFallidos n<4 --fallo--> n+1
intentosFallidos 4   --fallo--> bloqueadoHasta = ahora+15min, intentosFallidos = 0
cualquier estado     --ingreso correcto--> intentosFallidos = 0
bloqueado            --ahora > bloqueadoHasta o restablecer (CU21)--> desbloqueado
debeCambiarPassword  --CU22 correcto--> false ; --CU21 restablecer--> true
```

## Socio (hereda de Usuario, rol = SOCIO)

| Campo | Tipo | Reglas |
|---|---|---|
| codigoSocio | String(20) | obligatorio, único dentro de la junta |
| fechaIngreso | LocalDate | obligatorio |
| estado | EstadoSocio | por defecto ACTIVO |
| celular | String(10) | opcional |
| tieneCuenta | boolean | `false` hasta CU04; sin cuenta implica `activo = false` |
| sector | Sector | obligatorio, de la misma junta |

## Sector

| Campo | Tipo | Reglas |
|---|---|---|
| nombre | String(80) | obligatorio, único dentro de la junta |
| descripcion | String(200) | opcional |
| junta | Junta | obligatorio |

## SolicitudRecuperacion

| Campo | Tipo | Reglas |
|---|---|---|
| usuario | Usuario | obligatorio; nunca SUPER_ADMIN |
| junta | Junta | obligatorio; la junta del solicitante, copiada al crear (Principio II) |
| fechaSolicitud | LocalDateTime | se asigna al crear |
| estado | EstadoSolicitud | PENDIENTE al crear |
| fechaAtencion | LocalDateTime | se asigna al atender |
| atendidaPor | Usuario | ADMIN_JUNTA de la misma junta o SUPER_ADMIN |

Reglas: como máximo una solicitud PENDIENTE por usuario; `PENDIENTE → ATENDIDA` es la única
transición y es irreversible (FR-026). Visibilidad: solicitante SOCIO/SECRETARIO/TESORERO →
ADMIN_JUNTA de su junta (filtrando por `junta`); solicitante ADMIN_JUNTA → SUPER_ADMIN
(FR-024). Búsqueda del solicitante: primero por `username`, luego por `cedula` (FR-023).

## Notificacion

| Campo | Tipo | Reglas |
|---|---|---|
| usuario | Usuario | destinatario, obligatorio |
| titulo | String(120) | obligatorio |
| mensaje | String(300) | obligatorio |
| fecha | LocalDateTime | se asigna al crear |
| leida | boolean | por defecto `false` |
| enlace | String(200) | ruta interna relativa (empieza con `/`) |

Aislamiento: la notificación queda asociada a la junta a través de su destinatario
(`usuario.junta`); las del SUPER_ADMIN no tienen junta. Toda consulta filtra por el usuario
autenticado, lo que implica también su junta (Principio II).

Reglas: un usuario solo consulta y marca sus propias notificaciones (FR-036); orden: no leídas
primero, luego por fecha descendente (FR-033).
