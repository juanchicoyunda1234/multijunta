# Módulo 4: Gestión de Comunicaciones

Mockup: `docs/mockups/modulo4-CU15-CU19.png`.

## CU15 Emitir convocatorias
- Actores: Secretario/a (primario), Miembro/Socio (secundario). Prioridad alta.
- Disparador: botón "Redactar Convocatoria" en el Panel de Anuncios.
- Flujo: formulario con Título, Tipo (Asamblea, Minga), Fecha y Hora, y área de Orden del Día → clic en "Publicar Inmediatamente" → la convocatoria aparece en el inicio de todos los socios con un ícono de campana.
- Alterno: prioridad Normal con fecha de publicación futura; queda en BORRADOR y se publica sola en esa fecha.
- Excepción: fecha pasada se rechaza con "La fecha debe ser futura".
- Reglas: las convocatorias a Asamblea quedan fijadas arriba en la pantalla del socio hasta la fecha del evento.

## CU16 Archivar y publicar actas
- Actor: Secretario/a. Prioridad media.
- Disparador: botón "Subir Documento" en Repositorio/Actas.
- Flujo: formulario con Nombre del Acta, Fecha de Reunión, descripción y el archivo PDF → clic en "Guardar en Archivo" → el acta queda en la lista y visible en la pestaña "Repositorio" del socio (solo ver y descargar).
- Alterno: desmarcar "Público en Portal Socios" para que solo la vea la directiva.
- Excepción: archivos que no sean PDF se rechazan con "Formato no válido. Solo se admiten archivos PDF por seguridad."
- Reglas: los socios no pueden borrar ni modificar actas.

## CU17 Consultar información individual
- Actor: Miembro/Socio. Prioridad alta.
- Disparador: inicio de sesión del socio.
- Precondición: el socio tiene cuenta activa (CU04).
- Flujo: el socio inicia sesión → el sistema carga "Mi Panel" con menú "Resumen", "Estado de Cuenta", "Mi Predio" y "Calendario" → en "Estado de Cuenta" ve su Deuda Total en grande (verde si es cero, rojo si debe) y la tabla de sus últimos pagos.
- Alterno: "Mi Predio" muestra su predio en el mapa; "Calendario" sus próximos turnos de su sector.
- Excepción: credenciales incorrectas muestran "Usuario o contraseña inválidos".
- Reglas: todo es de solo lectura para el socio; no hay botones de guardar ni eliminar.

## CU18 Enviar y recibir mensajes
- Actores: Administrador de la Junta, Miembro/Socio. Prioridad media.
- Disparador: ícono "Mensajes" en la barra superior.
- Flujo: vista dividida, contactos a la izquierda y conversación en burbujas a la derecha → el socio elige el chat "Directiva", escribe y presiona "Enviar" → el mensaje aparece en su color y el destinatario ve el contador de no leídos aumentado.
- Alterno: adjuntar una imagen, que se muestra como miniatura en la burbuja.
- Excepción: con el texto vacío, el botón Enviar queda deshabilitado.
- Reglas: un socio solo puede escribir a la Directiva, nunca a otro socio; ambos usuarios deben ser de la misma junta. El contador se actualiza al recargar o cada pocos segundos (sin WebSocket).

## CU19 Registrar comunicaciones recibidas de otras instituciones
- Actor: Secretario/a. Prioridad media.
- Disparador: botón "Registrar Comunicación Recibida".
- Flujo: formulario con Institución remitente, Asunto, Fecha de recepción y Tipo de documento (Oficio, Notificación, Otro), más el documento PDF o imagen → clic en "Registrar Comunicación" → el sistema asigna un número correlativo de ingreso, guarda y lo muestra en el listado por fecha.
- Alterno: sin documento, queda "Pendiente de digitalización" y se adjunta después.
- Excepción: sin Institución o Asunto, el botón se bloquea y se resaltan los campos.
- Reglas: el número correlativo es único y secuencial por junta, no se edita ni se reutiliza aunque se elimine el registro.

---
# Casos de uso agregados en la fase de especificación

## CU29 Recibir notificaciones
- Actores: todos los usuarios. Prioridad media.
- Disparador: ícono de campana en la barra superior.
- Flujo: el sistema muestra la lista de notificaciones con las no leídas primero → el usuario abre una → el sistema la marca como leída y lo lleva a lo que corresponde.
- Origen de notificaciones: convocatoria publicada (CU15), turno de su sector creado o reprogramado (CU08), mensaje nuevo (CU18), multa o cuota nueva (CU10, CU30), solicitud de recuperación (CU21, solo administradores), corrección de asistencia por autorizar (CU28, solo administrador).
- Alterno: "Marcar todas como leídas".
- Reglas: el contador de la campana muestra solo las no leídas; se actualiza al cargar cada página.

## CU32 Consultar anuncios y repositorio
- Actor: Miembro/Socio. Prioridad media.
- Disparador: opciones "Anuncios" y "Repositorio" en el menú de "Mi Panel" (se suman a las del CU17).
- Flujo: en "Anuncios" ve las convocatorias publicadas, con las Asambleas fijadas arriba hasta su fecha; en "Repositorio" ve las actas públicas y puede abrirlas o descargarlas.
- Excepción: si no hay anuncios o actas, muestra "No hay publicaciones por ahora".
- Reglas: solo lectura; las actas marcadas solo para la directiva no aparecen.
