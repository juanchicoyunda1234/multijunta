# Research: Módulo Gestión Administrativa

Sin puntos NEEDS CLARIFICATION. Decisiones de diseño:

## R1. Interfaz IAsistencia

- **Decision**: interfaz Java `IAsistencia` en `service`, implementada por `AsistenciaService`,
  con dos operaciones de solo lectura: inasistencias de todos los eventos cerrados de una junta
  e inasistencias de un evento cerrado. Devuelve el record `Inasistencia` (ids y datos
  descriptivos), nunca entidades. Ver `contracts/iasistencia.md`.
- **Rationale**: es la interfaz del diagrama de componentes ("Gestión Administrativa expone
  IAsistencia; Gestión Financiera la usa"). Un contrato estable y sin entidades permite que CU10
  calcule multas sin conocer el modelo de asistencia, y que CU28 cambie un estado sin romper al
  consumidor.
- **Alternatives considered**: que el módulo financiero use `AsistenciaRepository` (acopla
  módulos); un endpoint HTTP (no hay frontend ni cliente externo que lo necesite).

## R2. Toma de asistencia

- **Decision**: una página con un formulario que lista los socios ACTIVOS con tres radios por
  socio (`estado[socioId]`), AUSENTE marcado por defecto; búsqueda por cédula o nombre filtrando
  filas en el cliente; "Cerrar Registro de Asistencia" con confirmación envía todo y el servicio,
  en una transacción, crea una `Asistencia` por socio listado, marca `asistenciaCerrada` y
  calcula el resumen. Un evento ya cerrado rechaza el envío (gana el primero).
- **Rationale**: cumple CU06 (sin guardado parcial) con un solo POST; 200 filas son triviales.

## R3. Calendario semanal de turnos

- **Decision**: `GET /turnos?semana=AAAA-MM-DD` (lunes de la semana) renderiza 5 columnas
  lunes–viernes; en cada columna los turnos ordenados por hora como bloques coloreados por sector
  (color estable derivado del id del sector). Cada columna tiene "+ Agendar" que abre un modal con
  fecha precargada, horas, sector, recurso activo y responsable. "Reprogramar" abre el mismo modal
  con datos. En celular las columnas se apilan.
- **Rationale**: cumple el mockup (vista semanal y micro-modal) sin librería de calendario.
- **Alternatives considered**: FullCalendar u otra librería (dependencia y JS extra sin
  necesidad).

## R4. Conflicto de recursos

- **Decision**: consulta `existsConflicto(recursoId, fecha, inicio, fin, excluirId)` con la
  condición de intervalos abiertos `inicio < finExistente AND fin > inicioExistente`; así los
  turnos que solo se tocan no chocan. Se valida en el servicio dentro de la transacción.

## R5. Notificaciones de turnos y correcciones

- **Decision**: reutilizar `NotificacionService.crear` de 001. Turnos: a cada socio con
  `tieneCuenta` y `activo` del sector del turno, con enlace `/notificaciones` (el socio no tiene
  acceso a `/turnos`). Correcciones: a cada ADMIN_JUNTA activo de la junta con enlace
  `/asistencia/correcciones`, y al solicitante con el resultado.

## R6. Mapa y fotos de predios (CU07)

- **Decision**: Leaflet 1.9.4 (CSS y JS por cdnjs/unpkg) con teselas de OpenStreetMap, en un
  fragmento `fragments/mapa.html`. Clic en el mapa coloca o mueve el pin y llena `latitud` y
  `longitud` ocultos. El servidor valida presencia y rango del Ecuador (latitud −5.1 a 2.0,
  longitud −92.1 a −75.1, cubre Galápagos). El perfil muestra un mapa pequeño no interactivo por
  predio. Fotos: `ArchivoService` se generaliza a `validarImagen(archivo, claveTamano, claveTipo,
  campo)` (firma PNG/JPG y 5 MB) y `guardar(carpeta, prefijo, ...)`; fotos en
  `./uploads/predios`, servidas por `GET /predios/{id}/foto` con control de junta.
- **Rationale**: tech.md fija Leaflet + OSM y guardar latitud/longitud; reutiliza la validación
  de logos de 002.

## R7. Códigos generados

- **Decision**: código de socio `S-0001` correlativo por junta (máximo numérico + 1, saltando
  códigos ocupados); código de predio `P-0001` correlativo por junta. Se generan en el servicio
  dentro de la transacción.
- **Rationale**: CU05 y CU07 no piden el código y el modelo lo exige.

## R8. Corrección de asistencia (CU28)

- **Decision**: `CorreccionAsistencia` con junta, asistencia, estados, motivo, estado de la
  solicitud, solicitante, resolutor y fechas. Solicitar: solo SECRETARIO, asistencia de evento
  cerrado de su junta, estado distinto, motivo obligatorio, sin otra PENDIENTE. Resolver: solo
  ADMIN_JUNTA. Aprobar cambia `Asistencia.estado` en la misma transacción. La lista
  `/asistencia/correcciones` muestra pendientes y el historial; no hay ruta de edición ni
  borrado. Los efectos sobre multas se agregan con el módulo financiero (spec, Supuestos).

## R9. Panel y regla de sectores

- **Decision**: `PanelService` obtiene "Próximos eventos y turnos" sumando eventos PROGRAMADOS y
  turnos, ambos con fecha de hoy a hoy+7 días (inclusive), y "Asistencias pendientes de cerrar" contando Asambleas/Mingas
  PROGRAMADAS de hoy o antes sin asistencia cerrada. `ConfiguracionJuntaService` amplía la
  comprobación de sectores a predios, recursos y turnos (nueva clave `sector.enUso`).

## R10. Datos de prueba

- **Decision**: `DatosPrueba` agrega por junta activa: 2 recursos, una Asamblea de ayer sin
  asistencia, una Minga en 5 días, una Reunión, 2 turnos de esta semana y un predio por socio
  activo con ubicación en su cantón. Mismo criterio de no duplicar.

## R11. Pruebas

- **Decision**: una clase MockMvc por CU con datos propios (`Fabrica` ampliada con evento,
  recurso, turno y predio). `AsistenciaTest` verifica `IAsistencia` directamente. Cada clase
  incluye un caso A/B entre juntas.
