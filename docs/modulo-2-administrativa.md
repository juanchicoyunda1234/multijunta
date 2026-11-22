# Módulo 2: Gestión Administrativa

Mockup: `docs/mockups/modulo2-CU05-CU08.png`.

## CU05 Registrar y mantener nómina
- Actor: Secretario/a. Prioridad alta.
- Disparador: botón "Añadir Nuevo Socio" en "Padrón de Miembros".
- Flujo: el sistema muestra un formulario en dos tarjetas, "Datos Personales" (cédula, nombres y apellidos) y "Datos de Contacto" (celular, sector desplegable) → clic en "Guardar Ficha de Socio" → vuelve a la tabla con el socio nuevo arriba.
- Excepción: si la cédula no tiene 10 dígitos o tiene letras, el botón Guardar se deshabilita y aparece un texto de ayuda.
- Reglas: todo socio debe tener un sector configurado en CU03; cédula única en la junta. También se puede buscar, editar y cambiar el estado (ACTIVO, INACTIVO).
- Fuera de alcance: el autocompletado de nombres por servicio externo de cédula.

## CU06 Registrar asistencia a eventos
- Actor: Secretario/a. Prioridad alta.
- Disparador: botón "Tomar Asistencia" en la tarjeta de un evento.
- Precondición: existe un evento (Asamblea o Minga) con fecha de hoy o pasada.
- Flujo: el sistema lista los socios activos con opciones [Presente] [Ausente] [Justificado], todos en Ausente por defecto → el secretario busca por cédula y marca → clic en "Cerrar Registro de Asistencia" → el sistema muestra un resumen (ej. 80 % de asistencia), guarda y bloquea el registro.
- Alterno: Justificado excluye al socio de las multas.
- Reglas: una vez cerrado, el registro no se edita. Expone la interfaz IAsistencia (inasistencias de eventos cerrados) que usa el CU10.
- Fuera de alcance: modo sin conexión (caché local).

## CU07 Registrar predios y localización en mapa
- Actor: Secretario/a. Prioridad media.
- Disparador: botón "Asignar Predio" en el perfil de un socio.
- Flujo: el sistema muestra Área (m²), Sector, un mapa Leaflet y una zona para subir foto → el secretario ingresa datos, coloca el pin y sube la foto JPG/PNG → clic en "Guardar Predio" → guarda latitud, longitud y foto, y vuelve al perfil con una miniatura del mapa.
- Alterno: puede guardar sin foto y agregarla después editando el predio.
- Excepción: foto mayor a 5 MB se rechaza con "El archivo excede el límite permitido".
- Reglas: no se guarda un predio sin pin en el mapa.

## CU08 Gestionar calendario y horarios
- Actor: Secretario/a. Prioridad alta.
- Disparador: módulo "Calendario de Servicios", vista semanal de lunes a viernes.
- Flujo: el secretario elige día y bloque de horas → el sistema abre un pequeño formulario para elegir Sector y Recurso (ej. "Compuerta #3") → clic en "Agendar" → aparece el bloque de color en el calendario.
- Alterno: reprogramar un turno existente cambiando su día u horas.
- Excepción: si el mismo recurso ya está asignado en ese horario a otro sector, se rechaza con "Conflicto de recursos detectado".
- Reglas: cada socio ve resaltados solo los turnos de su sector. También se crean aquí los eventos (Asamblea, Minga, Reunión) que usa el CU06.

---
# Casos de uso agregados en la fase de especificación

## CU26 Gestionar eventos
- Actor: Secretario/a. Prioridad alta.
- Disparador: botón "Nuevo Evento" en el Calendario de Actividades.
- Flujo: formulario con tipo (Asamblea, Minga, Reunión), fecha, hora, lugar y descripción → guarda → el evento aparece como tarjeta en el calendario con el botón "Tomar Asistencia" (CU06).
- Alterno: editar o cancelar un evento que todavía no tiene asistencia registrada.
- Excepción: no se puede editar ni cancelar un evento con asistencia cerrada.
- Reglas: al crear una Asamblea o Minga, el sistema ofrece crear de una vez su convocatoria (CU15).

## CU27 Gestionar recursos de riego
- Actor: Secretario/a. Prioridad media.
- Disparador: pestaña "Recursos" en el Calendario de Servicios.
- Flujo: el sistema lista los recursos (compuertas, canales, reservorios) → crea o edita uno con nombre, tipo, sector y caudal (L/s) → guarda.
- Alterno: desactivar un recurso; deja de aparecer en el CU08 pero sus turnos pasados se conservan.
- Excepción: nombre repetido en la misma junta se rechaza.
- Reglas: el selector "Recurso" del CU08 se llena con los recursos activos.

## CU28 Corregir asistencia cerrada
- Actor: Secretario/a (solicita), Administrador de la Junta (autoriza). Prioridad media.
- Disparador: botón "Solicitar corrección" en un registro de asistencia cerrado.
- Flujo: el secretario elige al socio, el nuevo estado (normalmente Justificado) y escribe el motivo obligatorio → el administrador aprueba → el sistema cambia el estado y guarda un registro de auditoría (quién, cuándo, estado anterior, estado nuevo, motivo).
- Alterno: el administrador rechaza la corrección y el registro queda igual.
- Excepción: si el socio ya tiene una multa por esa asistencia, al pasarlo a Justificado el sistema anula la multa si no tiene pagos; si ya tiene pagos, avisa al tesorero para el reverso (CU31).
- Reglas: el historial de auditoría es visible y no se edita.
