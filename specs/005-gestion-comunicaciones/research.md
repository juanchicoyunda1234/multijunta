# Research: Módulo Gestión de Comunicaciones

Decisiones técnicas de la funcionalidad 005. No quedan puntos "NEEDS CLARIFICATION".

## R1. Publicación programada de convocatorias (CU15 FR-007)

- **Decision**: `@EnableScheduling` en una clase de `config`. `ConvocatoriaService.publicarPendientes()`
  se anota con `@Scheduled(fixedDelay = 60000)`: publica los BORRADORES de todas las juntas con
  fecha de publicación vencida y evento futuro, y notifica. Se desactiva con la propiedad
  `multijunta.programador.habilitado` (en `false` en el perfil `test`). Las pruebas llaman al
  método directamente.
- **Rationale**: `docs/contexto/tech.md` fija `@Scheduled` para CU15. Una revisión por minuto
  cumple SC-003 sin otra infraestructura.
- **Alternatives considered**: publicar al cargar páginas (depende de que alguien entre);
  Quartz (dependencia nueva).

## R2. Conversaciones y estado de lectura (CU18)

- **Decision**: entidad `Conversacion` con dos formas:
  - socio con la directiva: `socio` presente, `miembroA` y `miembroB` vacíos;
  - entre dos miembros de la directiva: `socio` vacío, `miembroA` < `miembroB` por id, sin
    duplicados.

  Cada conversación guarda `ultimoLeidoA` (lo que leyó el socio o `miembroA`) y
  `ultimoLeidoB` (lo que leyó la directiva en conjunto o `miembroB`), como id del último
  mensaje leído. No leídos de un usuario = mensajes de sus conversaciones con id mayor a su
  marca y que no envió él (en las de socio, para la directiva: los enviados por el socio).
  `Mensaje` tiene conversación, remitente, contenido, imagen y fecha de envío.
- **Rationale**: cumple el estado de lectura compartido de "Directiva" (FR-018) sin una fila
  por miembro, y el conteo sale con una consulta agregada.
- **Alternatives considered**:
  - `Mensaje` con un destinatario por fila, como el modelo de dominio: duplica cada mensaje
    del socio por cada miembro de la directiva y complica la conversación compartida;
  - tabla de lecturas por usuario: no hace falta con dos lados por conversación.

  Se documenta la adaptación del modelo (`asunto` y `leido` no se usan por mensaje).

## R3. Actualización periódica sin conexión permanente (FR-019)

- **Decision**:
  - `GET /contadores` devuelve JSON `{"notificaciones": n, "mensajes": m}` del usuario. Un
    script del layout lo consulta cada 15 segundos y actualiza las insignias de la campana y de
    "Mensajes".
  - En una conversación abierta, el script pide `GET /mensajes/{id}/burbujas`, un fragmento
    Thymeleaf, cada 15 segundos y reemplaza la lista de burbujas.
  - `/contadores` se agrega a `MatrizPermisos` como ruta exacta para todos los roles con sesión
    (CU29).
- **Rationale**: CU18 pide "sin WebSocket". Sondeo simple, fácil de probar con MockMvc.
- **Alternatives considered**: WebSocket o SSE (descartados por CU18); recargar la página
  completa (pierde lo que el usuario escribe).

## R4. Número de ingreso de comunicaciones (CU19 FR-024)

- **Decision**: tabla `SecuenciaIngreso(junta, anio, ultimo)` con índice único `(junta, anio)`.
  Al registrar, se incrementa `ultimo` con bloqueo pesimista (`@Lock(PESSIMISTIC_WRITE)`) y se
  guarda `numeroIngreso = "CE-" + anio + "-" + %04d`. Eliminar la comunicación no toca la
  secuencia.
- **Rationale**: garantiza que el número no se reutilice aunque se elimine el último registro
  (FR-024, SC-006), cosa que no logra calcular `max + 1`.
- **Alternatives considered**: `count + 1` (reutiliza tras eliminar); secuencia global de la
  base (compartida entre juntas).

## R5. Archivos (actas, comunicaciones, imágenes de mensajes)

- **Decision**:
  - `ArchivoService` agrega `validarPdf(archivo, campo)` (bytes `%PDF`, 5 MB, clave
    `acta.formato`) y las subcarpetas `actas`, `comunicaciones` y `mensajes`.
  - Comunicaciones reusa `validarDocumento` (004); las imágenes de mensajes reusan
    `validarImagen` (003).
  - Se sirven desde la ruta del módulo solo a quien puede ver el registro: las actas públicas
    al socio por `/anuncios/repositorio/{id}/documento`; las imágenes solo a los participantes
    de la conversación.
  - `VistaComunAdvice` agrega `/actas`, `/comunicaciones` y `/mensajes` al manejo de archivo
    grande.
- **Rationale**: misma validación y almacenamiento de 002–004 (`./uploads`, 5 MB).

## R6. Pantallas del socio (CU17, CU32)

- **Decision**: `MiInformacionController` atiende:
  - `/mi-informacion` (Resumen);
  - `/mi-informacion/estado-cuenta`, `/mi-informacion/predio` y `/mi-informacion/calendario`;
  - `/mi-informacion/predio/{id}/foto`, porque el socio no tiene acceso a `/predios`.

  `AnuncioController` atiende `/anuncios` y `/anuncios/repositorio`. Las pestañas son un
  fragmento común de navegación. Reutilizan sin cambios:
  - `FinanzasService.deudaDeSocio`, `PagoRepository` y las obligaciones de la 004;
  - el fragmento de mapa de la 003;
  - `TurnoRepository`.

  El socio siempre se toma de la sesión: no hay ids de socio en las rutas.
- **Rationale**: las filas `/mi-informacion` y `/anuncios` de la matriz ya existen solo para
  SOCIO. Tomar el socio de la sesión elimina el riesgo de ver datos de otro socio (SC-005).

## R7. Notificaciones nuevas (CU29 FR-028)

- **Decision**: `NotificacionService.crear` se reutiliza. Se agregan llamadas en:
  - `ConvocatoriaService.publicar`: socios ACTIVOS con `tieneCuenta` y `activo`; enlace
    `/anuncios`;
  - `MensajeService.enviar`: destinatarios de la conversación; enlace `/mensajes/{id}`;
  - `CuotaService.emitir` y `MultaService.aprobar`: socio con cuenta activa; enlace
    `/mi-informacion/estado-cuenta`;
  - `TurnoService`: cambia el enlace del socio de `/notificaciones` a
    `/mi-informacion/calendario`.

  Los títulos van en `messages.properties`.
- **Rationale**: cierra los orígenes de CU29 sin tocar la base de 001.

## R8. Oferta de convocatoria desde Eventos (FR-011)

- **Decision**:
  - Al crear una Asamblea o Minga, `EventoController` agrega al flash el id del evento.
  - El calendario de eventos muestra "Crear convocatoria" con enlace a
    `/convocatorias/nueva?eventoId={id}`.
  - Esa ruta precarga tipo, fecha, hora y lugar (solo eventos de la junta; otro id responde
    404).
- **Rationale**: regla de CU26 diferida en 003; no crea dependencia del módulo administrativo
  hacia comunicaciones (solo un enlace).

## R9. Contraseña y credenciales del socio

- **Decision**: sin cambios: CU17 usa el inicio de sesión de 001 ("Usuario o contraseña
  inválidos") y la redirección a `/mi-informacion` que ya existe en `UsuarioActual.rutaInicio`.

## R10. Pantallas en construcción y pruebas afectadas

- **Decision**:
  - `EnConstruccionController` queda vacío de módulos: se eliminan sus rutas y la plantilla
    `en-construccion.html` permanece para `/en-construccion`.
  - `PermisosPorRolTest.moduloEnConstruccion` pasa a comprobar solo que `/en-construccion`
    responde a cualquier usuario con sesión (ya no hay módulos pendientes).
  - `PanelTest` ajusta la tarjeta "Mensajes no leídos", que deja de estar "Disponible
    próximamente".
- **Rationale**: es el último módulo; mismo patrón que en 003 y 004.

## R11. Datos de prueba

- **Decision**: por cada junta activa, `DatosPrueba` agrega:
  - una convocatoria a Asamblea publicada para dentro de 7 días;
  - una Minga en borrador programada para mañana;
  - un acta pública y una solo para la directiva, con PDF mínimo generado al arrancar en
    `./uploads/actas`;
  - dos comunicaciones externas (una pendiente de digitalización);
  - una conversación del socio con cuenta con la directiva, con dos mensajes (uno sin leer por
    la directiva).
- **Rationale**: permite probar cada historia a mano desde el primer arranque.
