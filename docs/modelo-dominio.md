# Modelo de dominio

Base: diagrama de clases del APE MDS 3. Todas las entidades llevan `Long id` autogenerado en lugar de los `idX : String` del diagrama. Los atributos marcados con (+) no estaban en el diagrama y se agregan porque las especificaciones de casos de uso los exigen.

## Entidades

| Clase | Atributos |
|---|---|
| Junta | activa (+), motivoSuspension (+), nombre, tipo (RIEGO, PARROQUIAL, COMUNITARIA), ruc (13 dígitos, único), direccion, telefono, fechaRegistro, provincia (+), canton (+), logoUrl (+), montoMultaInasistencia (+) |
| Usuario | nombres, apellidos, cedula, email, username, password, rol, activo (+), debeCambiarPassword (+), intentosFallidos (+), bloqueadoHasta (+) |
| Socio (hereda de Usuario) | codigoSocio, fechaIngreso, estado (ACTIVO, INACTIVO), celular (+), tieneCuenta (+) |
| Sector | nombre, descripcion |
| Predio | codigo, ubicacion, area, latitud (+), longitud (+), fotoNombre (+) |
| Evento | tipo (ASAMBLEA, MINGA, REUNION), fecha, hora, lugar, descripcion (+), estado (+) (PROGRAMADO, CANCELADO), asistenciaCerrada (+) |
| Asistencia | fecha, estado (PRESENTE, AUSENTE, JUSTIFICADO), observacion |
| Multa | motivo, monto, fecha, estado (PENDIENTE, PARCIAL, PAGADA, ANULADA), saldoPendiente (+) |
| Cuota (+) | concepto (ej. tarifa anual, medidor), monto, fecha, fechaVencimiento, estado (PENDIENTE, PARCIAL, PAGADA, ANULADA), saldoPendiente, motivoAnulacion |
| Pago | monto, fecha, concepto, metodoPago (EFECTIVO, TRANSFERENCIA), numeroTransaccion (+), reversado (+), motivoReverso (+) |
| Egreso | concepto, monto, fecha, categoria (MATERIALES, HONORARIOS, SERVICIOS_BASICOS, MANTENIMIENTO, OTROS), proveedor (+), comprobanteNombre (+), pendienteSoporte (+) |
| Inventario | nombre, cantidad, unidadMedida, estado (ACTIVO, INACTIVO), stockMinimo (+) |
| MovimientoInventario (+) | tipo (ENTRADA, SALIDA), cantidad, motivo, fecha |
| ActivoFijo | nombre, codigo, valorAdquisicion, fechaAdquisicion, estado (NUEVO, USADO, DADO_DE_BAJA), categoria (+) (MAQUINARIA, INFRAESTRUCTURA, EQUIPO_OFICINA, VEHICULOS), fotoNombre (+), motivoBaja (+) |
| Convocatoria | titulo, fecha, tipo (ASAMBLEA, MINGA), contenido (orden del día), hora (+), prioridad (+) (NORMAL, URGENTE), fechaPublicacion (+), estado (+) (BORRADOR, PUBLICADA) |
| Acta | titulo, fecha, contenido (descripción), tipoEvento, documentoNombre (+), publicaSocios (+) |
| ComunicacionExterna | remitente, asunto, fecha, contenido, numeroIngreso (+) (correlativo), tipoDocumento (+) (OFICIO, NOTIFICACION, OTRO), documentoNombre (+), pendienteDigitalizacion (+) |
| Mensaje | asunto, contenido, fechaEnvio, leido, imagenNombre (+) |
| Turno | fecha, horaInicio, horaFin, responsable |
| Recurso (+) | nombre (ej. "Compuerta #3"), tipo (COMPUERTA, CANAL, RESERVORIO), caudal, activo |
| SolicitudRecuperacion (+) | fechaSolicitud, estado (PENDIENTE, ATENDIDA), fechaAtencion |
| CorreccionAsistencia (+) | estadoAnterior, estadoNuevo, motivo, estado (PENDIENTE, APROBADA, RECHAZADA), fechaSolicitud, fechaResolucion |
| Notificacion (+) | titulo, mensaje, fecha, leida, enlace |

## Relaciones

| Relación | Multiplicidad |
|---|---|
| Junta gestiona Usuario | 1 a 0..* |
| Junta administra Sector | 1 a 0..* |
| Junta organiza Evento | 1 a 0..* |
| Junta registra Egreso | 1 a 0..* |
| Junta administra Inventario | 1 a 0..* |
| Junta administra ActivoFijo | 1 a 0..* |
| Junta emite Convocatoria | 1 a 0..* |
| Junta archiva Acta | 1 a 0..* |
| Junta recibe ComunicacionExterna | 1 a 0..* |
| Junta programa Turno | 1 a 0..* |
| Usuario envía/recibe Mensaje | 1 a 0..* (remitente y destinatario) |
| Socio hereda de Usuario | generalización |
| Socio pertenece a Sector (+) | 0..* a 1 (obligatorio) |
| Socio posee Predio | 1 a 0..* |
| Socio participa en Asistencia | 1 a 0..* |
| Socio recibe Multa | 1 a 0..* |
| Socio tiene Cuota (+) | 1 a 0..* |
| Socio realiza Pago | 1 a 0..* |
| Sector contiene Predio | 1 a 0..* |
| Sector tiene Turno (+) | 1 a 0..* |
| Evento registra Asistencia | 1 a 0..* |
| Asistencia genera Multa | 0..1 a 0..* |
| Multa se cancela con Pago | 0..1 a 0..* |
| Cuota se cancela con Pago (+) | 0..1 a 0..* |
| Inventario tiene MovimientoInventario (+) | 1 a 0..* (kardex) |
| Recurso tiene Turno (+) | 1 a 0..* |
| Sector tiene Recurso (+) | 1 a 0..* |
| Usuario hace SolicitudRecuperacion (+) | 1 a 0..* |
| Asistencia tiene CorreccionAsistencia (+) | 1 a 0..* (solicitada por un Secretario, resuelta por un Administrador) |
| Usuario recibe Notificacion (+) | 1 a 0..* |
| Pago cancela Multas y Cuotas (+) | un pago puede cubrir varias obligaciones; se guarda el detalle de cuánto aplicó a cada una para poder reversarlo |
