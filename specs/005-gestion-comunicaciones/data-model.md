# Data Model: Módulo Gestión de Comunicaciones

Todas las entidades llevan `Long id` autogenerado y `junta` (ManyToOne obligatorio), salvo
`Mensaje`, que la hereda de su conversación. Fechas `LocalDate`, horas `LocalTime`, instantes
`LocalDateTime`. Tablas creadas por JPA (`ddl-auto=update`).

## Enums

| Enum | Valores (etiqueta) |
|---|---|
| `TipoConvocatoria` | ASAMBLEA (Asamblea), MINGA (Minga) |
| `PrioridadConvocatoria` | NORMAL (Normal), URGENTE (Urgente) |
| `EstadoConvocatoria` | BORRADOR (Borrador), PUBLICADA (Publicada) |
| `TipoDocumentoExterno` | OFICIO (Oficio), NOTIFICACION (Notificación), OTRO (Otro) |
| `TipoEvento` (existente, 003) | se reutiliza como tipo de sesión del acta |

## Entidades

### Convocatoria (CU15)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| titulo | String(150) | obligatorio |
| tipo | TipoConvocatoria | obligatorio |
| fechaEvento | LocalDate | con `hora`, posterior al momento actual |
| hora | LocalTime | obligatoria |
| lugar | String(150) | obligatorio |
| contenido | String(4000) | orden del día, obligatorio |
| prioridad | PrioridadConvocatoria | obligatoria |
| fechaPublicacion | LocalDateTime | URGENTE: ahora; NORMAL: futura y anterior al evento |
| estado | EstadoConvocatoria | URGENTE nace PUBLICADA; NORMAL nace BORRADOR |
| autor | Usuario | secretario |

Transiciones: BORRADOR → PUBLICADA (programador o al guardar URGENTE). Solo BORRADOR se edita o
elimina.

Derivados:
- `fijada(hoy)` = tipo ASAMBLEA y fechaEvento ≥ hoy;
- `vencidaSinPublicar(ahora)` = BORRADOR y evento ya pasado.

### Acta (CU16)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| titulo | String(150) | obligatorio |
| fecha | LocalDate | fecha de reunión, no futura |
| tipoEvento | TipoEvento | obligatorio |
| contenido | String(1000) | descripción, opcional |
| documentoNombre | String | PDF en `uploads/actas`, obligatorio |
| publicaSocios | boolean | por defecto true |

### ComunicacionExterna (CU19)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| numeroIngreso | String(14) | `CE-AAAA-NNNN`, único por junta (R4) |
| remitente | String(150) | obligatorio |
| asunto | String(200) | obligatorio |
| fecha | LocalDate | fecha de recepción, no futura |
| tipoDocumento | TipoDocumentoExterno | obligatorio |
| documentoNombre | String | `uploads/comunicaciones`; `null` significa "Pendiente de digitalización" |
| registradoPor | Usuario | secretario |

### SecuenciaIngreso (CU19)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| anio | int | único con `junta` |
| ultimo | int | último número asignado; nunca baja |

### Conversacion (CU18)

| Campo | Tipo | Reglas |
|---|---|---|
| junta | Junta | obligatorio |
| socio | Socio | presente en conversaciones socio–directiva; única por socio |
| miembroA | Usuario | presente en conversaciones entre directiva, con id menor |
| miembroB | Usuario | con id mayor; par único |
| ultimoLeidoA | Long | id del último mensaje leído por el socio o `miembroA` |
| ultimoLeidoB | Long | id del último mensaje leído por la directiva o `miembroB` |
| actualizada | LocalDateTime | fecha del último mensaje, para ordenar contactos |

Regla: exactamente una de las dos formas (socio, o miembroA + miembroB).

### Mensaje (CU18)

| Campo | Tipo | Reglas |
|---|---|---|
| conversacion | Conversacion | obligatorio |
| remitente | Usuario | participante de la conversación |
| contenido | String(2000) | opcional si hay imagen |
| imagenNombre | String | JPG/PNG en `uploads/mensajes`, opcional |
| fechaEnvio | LocalDateTime | instante de envío |

Regla: contenido no vacío o imagen.

Adaptación del modelo de dominio: `Mensaje.asunto` no se usa (es un chat) y `Mensaje.leido` se
reemplaza por las marcas de lectura de la conversación (research R2).

## Cambios a entidades existentes

- `Notificacion` (001): sin cambios de columnas; nuevos títulos y enlaces.
- `CuotaService` y `MultaService` (004) y `TurnoService` (003): generan notificaciones nuevas o
  cambian el enlace (research R7).
- `MatrizPermisos`: nueva entrada exacta `/contadores` para todos los roles con sesión
  (research R3).

## Consultas agregadas

| Valor | Definición |
|---|---|
| Mensajes no leídos (socio) | mensajes de su conversación con id > `ultimoLeidoA` y remitente ≠ el socio |
| Mensajes no leídos (directiva) | mensajes de conversaciones de socio de su junta con id > `ultimoLeidoB` y remitente = el socio, más los de sus conversaciones entre directiva con id mayor a su marca y remitente ≠ él |
| Anuncios del socio | convocatorias PUBLICADAS de la junta, primero las que están fijadas, luego por fechaPublicacion descendente |
