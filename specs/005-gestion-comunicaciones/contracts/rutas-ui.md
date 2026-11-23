# Contrato: rutas de la interfaz (005)

Las rutas de 001 a 004 siguen vigentes. Respuestas HTML en español; los POST llevan CSRF.

Los permisos salen de las filas de `MatrizPermisos`: los GET los ven los roles con acceso
completo (C) o de lectura (L), y los POST solo los roles C. Un identificador de otra junta, o de
otro socio en las rutas del socio, responde 404.

| Fila de la matriz | C | L |
|---|---|---|
| `/convocatorias` (CU15) | SECRETARIO | ADMIN_JUNTA |
| `/actas` (CU16) | SECRETARIO | ADMIN_JUNTA, TESORERO |
| `/comunicaciones` (CU19) | SECRETARIO | ADMIN_JUNTA |
| `/mi-informacion` (CU17) | SOCIO | — |
| `/anuncios` (CU32) | SOCIO | — |
| `/mensajes` (CU18) | ADMIN_JUNTA, SECRETARIO, TESORERO, SOCIO | — |
| `/contadores` (CU29, **nueva, exacta**) | todos con sesión | — |

## Mi información (CU17)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/mi-informacion` | Resumen: ficha, deuda total, próximo turno, convocatorias recientes con Asambleas fijadas | CU17 FR-001 |
| GET `/mi-informacion/estado-cuenta` | deuda total en verde o rojo, obligaciones pendientes, últimos 10 pagos, botón "Imprimir estado de cuenta" | CU17 FR-002 |
| GET `/mi-informacion/predio` | predios del socio en el mapa con código, área, sector y foto | CU17 FR-003 |
| GET `/mi-informacion/predio/{id}/foto` | foto de un predio **del socio**; otro predio responde 404 | CU17 FR-003 |
| GET `/mi-informacion/calendario` | turnos de su sector desde hoy hasta 4 semanas después | CU17 FR-003 |

## Anuncios y repositorio (CU32)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/anuncios` | convocatorias PUBLICADAS, Asambleas fijadas primero, o "No hay publicaciones por ahora" | CU32 FR-012, FR-013 |
| GET `/anuncios/repositorio` | actas públicas, o "No hay publicaciones por ahora" | CU32 FR-012, FR-013 |
| GET `/anuncios/repositorio/{id}/documento` | PDF de un acta pública; una privada responde 404 | CU32 FR-013 |

## Convocatorias (CU15)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/convocatorias` | lista con estado, prioridad, fecha de publicación y "Vencida sin publicar"; botón "Redactar Convocatoria" | CU15 |
| GET `/convocatorias/nueva?eventoId=` | formulario; con `eventoId` precarga tipo, fecha, hora y lugar del evento de la junta | CU15 FR-005, FR-011 |
| POST `/convocatorias` | crea: URGENTE publica y notifica; NORMAL queda en borrador | CU15 FR-005..008 |
| GET `/convocatorias/{id}/editar` y POST `/convocatorias/{id}` | edita un borrador; una publicada responde con "Una convocatoria publicada no se puede modificar" | CU15 FR-010 |
| POST `/convocatorias/{id}/eliminar` | elimina un borrador | CU15 FR-010 |

## Actas (CU16)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/actas` | lista de actas con visibilidad; botón "Subir Documento" | CU16 |
| POST `/actas` (multipart) | `titulo`, `fecha`, `tipoEvento`, `contenido`, `documento`, `publicaSocios` | CU16 FR-020, FR-021 |
| POST `/actas/{id}/visibilidad` | `publicaSocios=true\|false` | CU16 FR-022 |
| GET `/actas/{id}/documento` | PDF del acta para la directiva | CU16 |

## Comunicaciones externas (CU19)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/comunicaciones` | listado por fecha de recepción descendente con número de ingreso y "Pendiente de digitalización"; botón "Registrar Comunicación Recibida" | CU19 |
| POST `/comunicaciones` (multipart) | `remitente`, `asunto`, `fecha`, `tipoDocumento`, `documento` | CU19 FR-023..026 |
| POST `/comunicaciones/{id}/documento` (multipart) | adjunta el documento | CU19 FR-025 |
| GET `/comunicaciones/{id}/documento` | sirve el documento; 404 si no hay | CU19 |
| POST `/comunicaciones/{id}/eliminar` | elimina sin liberar el número | CU19 FR-024, FR-027 |

## Mensajes (CU18)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/mensajes` | vista dividida: contactos con buscador, último mensaje y no leídos | CU18 FR-014, FR-015 |
| GET `/mensajes/{id}` | conversación abierta: marca como leídos los recibidos | CU18 FR-018 |
| GET `/mensajes/{id}/burbujas` | fragmento con las burbujas; lo pide el script cada 15 s | CU18 FR-019 |
| POST `/mensajes/{id}` (multipart) | `contenido`, `imagen`; valida participante, junta y contenido | CU18 FR-016, FR-017 |
| POST `/mensajes/nueva` | `usuarioId`: la directiva abre o crea la conversación con un socio o miembro de su junta; un socio solo obtiene su "Directiva" | CU18 FR-015, FR-017 |
| GET `/mensajes/imagen/{mensajeId}` | imagen solo para participantes; si no, 404 | CU18 |

## Contadores (CU29, CU18)

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/contadores` | JSON `{"notificaciones": n, "mensajes": m}` del usuario en sesión | CU29, CU18 FR-019 |

## Panel (CU25)

La tarjeta "Mensajes no leídos" (enlace `/mensajes`) muestra el número real (FR-031).
