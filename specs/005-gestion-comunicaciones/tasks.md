---

description: "Lista de tareas de la funcionalidad 005-gestion-comunicaciones"
---

# Tasks: Módulo Gestión de Comunicaciones

**Input**: Design documents from `/specs/005-gestion-comunicaciones/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: incluidas. La constitución (Principio VII) exige verificar cada CU contra sus
criterios de aceptación; `plan.md` define una clase de prueba por CU. Se escriben antes de
implementar cada historia y deben fallar primero.

**Organization**: tareas agrupadas por historia de `spec.md`; cada tarea indica su CU
(Principio VIII). Se reutilizan 001 a 004 sin duplicarlas.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia de `spec.md` (US1 a US7)

## Path Conventions

- Código: `src/main/java/ec/uta/multijunta/` (abreviado `JAVA/`)
- Recursos: `src/main/resources/` (abreviado `RES/`)
- Pruebas: `src/test/java/ec/uta/multijunta/` (abreviado `TEST/`)
- Sin comentarios en el código (Principio VI); textos en español (Principio IV)

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 [P] Agregar a `RES/messages.properties` todas las claves de `specs/005-gestion-comunicaciones/contracts/mensajes.md`
- [X] T002 [P] Agregar a `RES/static/css/app.css` estilos de burbujas de mensaje (propias a la derecha en verde, ajenas a la izquierda), vista dividida de mensajes que en celular muestra contactos o conversación, pestañas del socio desplazables, deuda en verde y en rojo, y convocatoria fijada

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de terminar esta fase

- [X] T003 [P] Crear enums `JAVA/model/TipoConvocatoria.java`, `JAVA/model/PrioridadConvocatoria.java`, `JAVA/model/EstadoConvocatoria.java` y `JAVA/model/TipoDocumentoExterno.java` con los valores y etiquetas de data-model.md (Enums)
- [X] T004 [P] Crear `JAVA/model/Convocatoria.java` y `JAVA/repository/ConvocatoriaRepository.java` con data-model.md Convocatoria.junta, Convocatoria.titulo, Convocatoria.tipo, Convocatoria.fechaEvento, Convocatoria.hora, Convocatoria.lugar, Convocatoria.contenido, Convocatoria.prioridad, Convocatoria.fechaPublicacion, Convocatoria.estado y Convocatoria.autor, más los derivados `fijada(hoy)` y `vencidaSinPublicar(ahora)`; consultas por junta (todas, más recientes primero), publicadas por junta, por id y junta, y borradores de todas las juntas con fecha de publicación vencida
- [X] T005 [P] Crear `JAVA/model/Acta.java` y `JAVA/repository/ActaRepository.java` con data-model.md Acta.junta, Acta.titulo, Acta.fecha, Acta.tipoEvento, Acta.contenido, Acta.documentoNombre y Acta.publicaSocios; consultas por junta, públicas por junta, por id y junta, y por id, junta y pública
- [X] T006 [P] Agregar a `JAVA/service/ArchivoService.java` `validarPdf(archivo, campo)` (bytes `%PDF`, 5 MB, claves `acta.formato` y `archivo.grande`) y las subcarpetas `actas`, `comunicaciones` y `mensajes` (research.md R5); mantener verdes las pruebas de 002 a 004
- [X] T007 [P] Ampliar `TEST/Fabrica.java` con `convocatoria(junta, tipo, fechaEvento, estado)`, `acta(junta, titulo, publica)` (con archivo PDF mínimo en la carpeta de pruebas) y `comunicacion(junta, remitente)`
- [X] T008 Cambiar `moduloEnConstruccion` en `TEST/PermisosPorRolTest.java` para que solo compruebe que `/en-construccion` responde a cualquier usuario con sesión (research.md R10). Cada historia retira su ruta de `JAVA/rest/EnConstruccionController.java`: T019, T027, T036, T042 y T049

**Checkpoint**: compila y las pruebas de 001 a 004 siguen en verde

---

## Phase 3: User Story 1 - Consultar información individual (Priority: P1) 🎯 MVP — CU17

**Goal**: el socio ve su resumen, estado de cuenta, predio y calendario en solo lectura.

**Independent Test**: `MiInformacionTest` en verde; pasos 1 a 3 de quickstart.md.

### Tests for User Story 1 ⚠️

- [X] T009 [P] [US1] Escribir `TEST/MiInformacionTest.java` (CU17) con HU1-1 a HU1-8 de spec.md:
  - inicio de sesión del socio lleva a `/mi-informacion` y muestra las 7 pestañas;
  - resumen con ficha, deuda, próximo turno y convocatorias (Asamblea fijada primero);
  - estado de cuenta en verde con deuda cero y en rojo con deuda, con detalle y últimos 10 pagos (uno reversado);
  - predio con foto servida solo si es del socio (la de otro socio da 404) y sin predios;
  - calendario con turnos de su sector dentro de 4 semanas (sin los de otro sector ni los de más adelante);
  - ausencia de formularios de guardar o eliminar y 403 al hacer POST;
  - directiva con 403 en `/mi-informacion`;
  - datos de otro socio nunca presentes

### Implementation for User Story 1

- [X] T010 [P] [US1] Agregar a `JAVA/repository/PagoRepository.java` los últimos 10 pagos de un socio. Agregar a `JAVA/repository/TurnoRepository.java` los turnos de un sector entre dos fechas, ordenados por fecha y hora
- [X] T011 [US1] Crear `JAVA/service/MiInformacionService.java` (CU17), que toma el socio de la sesión (research.md R6) y ofrece:
  - `resumen()`: ficha, deuda, próximo turno y convocatorias publicadas con data-model.md Convocatoria.estado y `fijada`;
  - `estadoCuenta()`: deuda vía `FinanzasService`, obligaciones de la 004 y últimos pagos;
  - `predios()` y `foto(predioId)`, solo predios del socio;
  - `calendario()`: turnos de su sector a 4 semanas
- [X] T012 [US1] Crear `JAVA/rest/MiInformacionController.java` con las rutas `/mi-informacion` de contracts/rutas-ui.md. Quitar `/mi-informacion` de `JAVA/rest/InicioController.java` y eliminar `RES/templates/inicio/socio.html`
- [X] T013 [US1] Crear `RES/templates/fragments/socio.html` (pestañas de Mi Panel) y las vistas:
  - `RES/templates/socio/resumen.html`, que incluye el fragmento `mensajes` del layout, para que se
    sigan viendo los avisos que llegan al socio tras cambiar su contraseña (001);
  - `RES/templates/socio/estado-cuenta.html` (deuda en grande en verde o rojo, botón "Imprimir estado de cuenta" con estilos de impresión);
  - `RES/templates/socio/predio.html` (fragmento de mapa de 003);
  - `RES/templates/socio/calendario.html`
- [X] T014 [US1] Verificar en `TEST/MiInformacionTest.java` que el menú del socio, que sale de las entradas que ya existen en `JAVA/config/MatrizPermisos.java`, muestra solo "Mi información", "Anuncios y repositorio" y "Mensajes", y que cada uno abre su pantalla. Sin cambios en `MatrizPermisos` salvo que la prueba lo exija
- [X] T015 [US1] Ejecutar `TEST/MiInformacionTest.java` y las pruebas de 001 y 002 que usan la página del socio (`TEST/CambioPasswordTest.java`, `TEST/PermisosPorRolTest.java`, `TEST/CuentasSociosTest.java`, `TEST/RecuperacionTest.java`, `TEST/LoginTest.java` y `TEST/ConvergenciaTest.java`) hasta que pasen

**Checkpoint**: el socio tiene su panel

---

## Phase 4: User Story 2 - Emitir convocatorias (Priority: P1) — CU15

**Goal**: el SECRETARIO publica o programa convocatorias y los socios las reciben.

**Independent Test**: `ConvocatoriaTest` en verde; pasos 7 a 10 de quickstart.md.

### Tests for User Story 2 ⚠️

- [X] T016 [P] [US2] Escribir `TEST/ConvocatoriaTest.java` (CU15) con HU2-1 a HU2-8 de spec.md:
  - formulario con prioridad y lugar;
  - Urgente publica al momento y notifica solo a socios ACTIVOS con cuenta activa de la junta;
  - Normal queda en borrador y `publicarPendientes()` la publica al llegar la fecha y notifica;
  - fecha de evento pasada ("La fecha debe ser futura");
  - fecha de publicación pasada o posterior al evento;
  - borrador editable y eliminable;
  - publicada no editable;
  - borrador con evento ya pasado queda "Vencida sin publicar" y no se publica;
  - oferta desde Eventos con datos precargados (evento de otra junta da 404);
  - ADMIN_JUNTA con lectura y 403;
  - otra junta con 404;
  - `ConvocatoriaService.publicarPendientes()` tiene `@Scheduled` con `fixedDelay` de 60000 ms o
    menos, comprobado por reflexión (SC-003)

### Implementation for User Story 2

- [X] T017 [P] [US2] Crear `JAVA/service/DatosConvocatoria.java` y `JAVA/rest/formulario/ConvocatoriaFormulario.java` con validaciones de data-model.md Convocatoria.titulo, Convocatoria.tipo, Convocatoria.fechaEvento, Convocatoria.hora, Convocatoria.lugar, Convocatoria.contenido, Convocatoria.prioridad y Convocatoria.fechaPublicacion
- [X] T018 [US2] Crear la configuración del programador:
  - `JAVA/config/ProgramadorConfig.java` con `@EnableScheduling`, condicionado por `multijunta.programador.habilitado`;
  - la propiedad en `RES/application.properties` (true) y en `src/test/resources/application-test.properties` (false).

  Crear `JAVA/service/ConvocatoriaService.java` (CU15) con:
  - `crear`, `editar`, `eliminar` (solo BORRADOR, data-model.md Convocatoria.estado);
  - `publicar`, que fija Convocatoria.fechaPublicacion y notifica (research.md R7);
  - `publicarPendientes()` con `@Scheduled(fixedDelay = 60000)` (research.md R1);
  - `precargar(eventoId)` vía `EventoRepository` por junta
- [X] T019 [US2] Crear `JAVA/rest/ConvocatoriaController.java` con las rutas `/convocatorias` de contracts/rutas-ui.md y las vistas `RES/templates/convocatorias/lista.html` y `RES/templates/convocatorias/formulario.html` (mockup: título, tipo, fecha y hora, lugar, prioridad Urgente/Normal con fecha de publicación, orden del día, "Publicar Inmediatamente"). Quitar `/convocatorias` de `JAVA/rest/EnConstruccionController.java`
- [X] T020 [US2] Ofrecer "Crear convocatoria" tras guardar una Asamblea o Minga en `JAVA/rest/EventoController.java` y `RES/templates/eventos/calendario.html` (research.md R8), sin romper `TEST/EventoTest.java`
- [X] T021 [US2] Ejecutar `TEST/ConvocatoriaTest.java` y `TEST/EventoTest.java` hasta que pasen

**Checkpoint**: la junta convoca a sus socios

---

## Phase 5: User Story 3 - Consultar anuncios y repositorio (Priority: P1) — CU32

**Goal**: el socio ve convocatorias publicadas y actas públicas.

**Independent Test**: `AnuncioTest` en verde; paso 4 de quickstart.md.

### Tests for User Story 3 ⚠️

- [X] T022 [P] [US3] Escribir `TEST/AnuncioTest.java` (CU32) con HU3-1 a HU3-5 de spec.md:
  - anuncios con la Asamblea futura fijada arriba y el resto por fecha de publicación;
  - borradores ausentes;
  - repositorio con actas públicas y descarga del PDF;
  - acta privada ausente y su descarga con 404;
  - "No hay publicaciones por ahora" en ambas secciones vacías;
  - sin acciones de modificar;
  - directiva con 403;
  - datos de otra junta ausentes

### Implementation for User Story 3

- [X] T023 [US3] Crear `JAVA/service/AnuncioService.java` (CU32): `anuncios()`, ordenados según data-model.md Consultas agregadas; `repositorio()`, solo data-model.md Acta.publicaSocios; `documento(actaId)`, que sirve solo actas públicas de la junta del socio
- [X] T024 [US3] Crear `JAVA/rest/AnuncioController.java` con las rutas `/anuncios` de contracts/rutas-ui.md
- [X] T025 [US3] Crear `RES/templates/socio/anuncios.html` y `RES/templates/socio/repositorio.html` con las pestañas de `RES/templates/fragments/socio.html`
- [X] T026 [US3] Reutilizar en `JAVA/service/MiInformacionService.java` el orden de `AnuncioService` para las convocatorias del Resumen
- [X] T027 [US3] Quitar `/anuncios` de `JAVA/rest/EnConstruccionController.java` y ejecutar `TEST/AnuncioTest.java` y `TEST/MiInformacionTest.java` hasta que pasen

**Checkpoint**: el socio recibe la información pública de la junta

---

## Phase 6: User Story 4 - Enviar y recibir mensajes (Priority: P2) — CU18

**Goal**: socios y directiva conversan con texto e imagen y ven contadores actualizados.

**Independent Test**: `MensajeTest` en verde; pasos 5 y 6 de quickstart.md.

### Tests for User Story 4 ⚠️

- [X] T028 [P] [US4] Escribir `TEST/MensajeTest.java` (CU18) con HU4-1 a HU4-8 de spec.md:
  - vista dividida con contactos;
  - socio con único contacto "Directiva";
  - envío del socio que llega a los tres roles de la directiva, con notificación a cada uno;
  - respuesta de un miembro vista como "Directiva" por el socio;
  - conversaciones entre miembros de la directiva;
  - texto vacío rechazado ("Escriba un mensaje") y presencia del script que deshabilita Enviar;
  - imagen PNG con miniatura;
  - imagen de otro tipo rechazada;
  - imagen visible solo para participantes (otro, 404);
  - socio que intenta escribir a otro socio o a otra junta con 403 o 404;
  - lectura compartida de "Directiva";
  - `/contadores` con JSON correcto que baja al abrir la conversación;
  - el HTML de cualquier página con sesión incluye el script que consulta `/contadores` cada
    15000 ms, y `mensajes/vista.html` incluye el que refresca `/mensajes/{id}/burbujas` (FR-019,
    SC-004);
  - un miembro de la directiva desactivado deja de aparecer como contacto y no recibe la
    notificación de los mensajes nuevos de "Directiva" (Edge Cases);
  - fragmento `/mensajes/{id}/burbujas`;
  - tarjeta "Mensajes no leídos" del panel con el número real

### Implementation for User Story 4

- [X] T029 [P] [US4] Crear `JAVA/model/Conversacion.java`, `JAVA/model/Mensaje.java`, `JAVA/repository/ConversacionRepository.java` y `JAVA/repository/MensajeRepository.java`:
  - campos data-model.md Conversacion.junta, Conversacion.socio, Conversacion.miembroA, Conversacion.miembroB, Conversacion.ultimoLeidoA, Conversacion.ultimoLeidoB y Conversacion.actualizada;
  - campos Mensaje.conversacion, Mensaje.remitente, Mensaje.contenido, Mensaje.imagenNombre y Mensaje.fechaEnvio;
  - conversación por socio, por par de miembros y por junta;
  - mensajes por conversación;
  - conteo de no leídos según data-model.md Consultas agregadas
- [X] T030 [US4] Crear `JAVA/service/MensajeService.java` (CU18):
  - `contactos(texto)`;
  - `abrir(conversacionId)`, que marca como leído (research.md R2);
  - `enviar(conversacionId, contenido, imagen)`, que valida participante, junta, regla socio–directiva y contenido o imagen (`validarImagen`), y notifica a los destinatarios (research.md R7);
  - `iniciar(usuarioId)`;
  - `imagen(mensajeId)`, solo para participantes;
  - `noLeidos()`
- [X] T031 [US4] Crear `JAVA/rest/MensajeController.java` con las rutas `/mensajes` de contracts/rutas-ui.md, y `JAVA/rest/ContadorController.java` con GET `/contadores` en JSON
- [X] T032 [US4] Agregar a `JAVA/config/MatrizPermisos.java` la entrada exacta `/contadores` para todos los roles con sesión (research.md R3)
- [X] T033 [US4] Crear `RES/templates/mensajes/vista.html` (vista dividida del mockup, buscador de contactos, formulario con imagen y script que deshabilita "Enviar" sin texto ni imagen) y `RES/templates/mensajes/burbujas.html` (fragmento que se refresca cada 15 s)
- [X] T034 [US4] Agregar en `RES/templates/fragments/layout.html`:
  - el ícono "Mensajes" con contador para los roles con acceso;
  - el script común que consulta `/contadores` cada 15 s y actualiza campana y mensajes.

  En `JAVA/rest/VistaComunAdvice.java`:
  - agregar el atributo `mensajesNoLeidos`;
  - agregar `/mensajes` al manejo de archivo grande
- [X] T035 [US4] Completar en `JAVA/service/PanelService.java` la tarjeta "Mensajes no leídos" con `MensajeService.noLeidos()` (FR-031) y ajustar `TEST/PanelTest.java`
- [X] T036 [US4] Quitar `/mensajes` de `JAVA/rest/EnConstruccionController.java` y ejecutar `TEST/MensajeTest.java` y `TEST/PanelTest.java` hasta que pasen

**Checkpoint**: socios y directiva conversan

---

## Phase 7: User Story 5 - Archivar y publicar actas (Priority: P2) — CU16

**Goal**: el SECRETARIO archiva actas en PDF y decide si las ven los socios.

**Independent Test**: `ActaTest` en verde; paso 11 de quickstart.md.

### Tests for User Story 5 ⚠️

- [X] T037 [P] [US5] Escribir `TEST/ActaTest.java` (CU16) con HU5-1 a HU5-7 de spec.md:
  - formulario con "Público en Portal Socios" marcado;
  - acta pública visible en el repositorio del socio;
  - acta privada solo para la directiva;
  - PNG rechazado con el mensaje exacto;
  - fecha futura rechazada;
  - cambio de visibilidad;
  - socio con 403 al hacer POST;
  - ADMIN_JUNTA y TESORERO con lectura y descarga, y 403 al modificar;
  - acta de otra junta con 404;
  - POST `/actas/{id}/eliminar` responde 404 o 405 y el acta sigue existiendo (FR-022)

### Implementation for User Story 5

- [X] T038 [P] [US5] Crear `JAVA/service/DatosActa.java` y `JAVA/rest/formulario/ActaFormulario.java` con validaciones de data-model.md Acta.titulo, Acta.fecha, Acta.tipoEvento, Acta.contenido y Acta.publicaSocios
- [X] T039 [US5] Crear `JAVA/service/ActaService.java` (CU16): `guardar` con `validarPdf` en data-model.md Acta.documentoNombre, `cambiarVisibilidad`, `listar` y `documento`
- [X] T040 [US5] Crear `JAVA/rest/ActaController.java` con las rutas `/actas` de contracts/rutas-ui.md y agregar `/actas` al manejo de archivo grande de `JAVA/rest/VistaComunAdvice.java`
- [X] T041 [US5] Crear `RES/templates/actas/lista.html` (lista, modal "Subir Documento" con los campos del mockup y "Guardar en Archivo", interruptor de visibilidad para SECRETARIO)
- [X] T042 [US5] Quitar `/actas` de `JAVA/rest/EnConstruccionController.java` y ejecutar `TEST/ActaTest.java` y `TEST/AnuncioTest.java` hasta que pasen

**Checkpoint**: las actas quedan archivadas y publicadas

---

## Phase 8: User Story 6 - Registrar comunicaciones recibidas (Priority: P2) — CU19

**Goal**: el SECRETARIO registra correspondencia externa con número de ingreso que no se reutiliza.

**Independent Test**: `ComunicacionExternaTest` en verde; paso 12 de quickstart.md.

### Tests for User Story 6 ⚠️

- [X] T043 [P] [US6] Escribir `TEST/ComunicacionExternaTest.java` (CU19) con HU6-1 a HU6-7 de spec.md:
  - registro con PDF y número `CE-AAAA-0001`;
  - orden por fecha de recepción;
  - sin documento queda "Pendiente de digitalización" y se adjunta después;
  - institución o asunto vacíos rechazados, con el script que deshabilita el botón;
  - fecha futura rechazada;
  - eliminación sin reutilizar el número;
  - numeración independiente por junta;
  - archivo inválido;
  - ADMIN_JUNTA con lectura y 403;
  - otra junta con 404

### Implementation for User Story 6

- [X] T044 [P] [US6] Crear `JAVA/model/ComunicacionExterna.java`, `JAVA/model/SecuenciaIngreso.java`, `JAVA/repository/ComunicacionExternaRepository.java` y `JAVA/repository/SecuenciaIngresoRepository.java`:
  - campos data-model.md ComunicacionExterna.junta, ComunicacionExterna.numeroIngreso, ComunicacionExterna.remitente, ComunicacionExterna.asunto, ComunicacionExterna.fecha, ComunicacionExterna.tipoDocumento, ComunicacionExterna.documentoNombre y ComunicacionExterna.registradoPor;
  - campos SecuenciaIngreso.junta, SecuenciaIngreso.anio y SecuenciaIngreso.ultimo;
  - consulta con bloqueo pesimista (research.md R4)
- [X] T045 [P] [US6] Crear `JAVA/service/DatosComunicacion.java` y `JAVA/rest/formulario/ComunicacionFormulario.java` con validaciones de data-model.md ComunicacionExterna.remitente, ComunicacionExterna.asunto, ComunicacionExterna.fecha y ComunicacionExterna.tipoDocumento
- [X] T046 [US6] Crear `JAVA/service/ComunicacionExternaService.java` (CU19):
  - `registrar`: siguiente número vía SecuenciaIngreso.ultimo y documento con `validarDocumento`;
  - `adjuntar`, `eliminar` (sin tocar la secuencia), `listar` y `documento`
- [X] T047 [US6] Crear `JAVA/rest/ComunicacionExternaController.java` con las rutas `/comunicaciones` de contracts/rutas-ui.md y agregar `/comunicaciones` al manejo de archivo grande de `JAVA/rest/VistaComunAdvice.java`
- [X] T048 [US6] Crear `RES/templates/comunicaciones/lista.html`:
  - listado;
  - modal "Registrar Comunicación Recibida" con los campos del mockup y el script que deshabilita "Registrar Comunicación" y resalta los campos sin institución o asunto;
  - adjuntar documento en modal;
  - eliminar con confirmación
- [X] T049 [US6] Quitar `/comunicaciones` de `JAVA/rest/EnConstruccionController.java` y ejecutar `TEST/ComunicacionExternaTest.java` hasta que pase

**Checkpoint**: la correspondencia externa queda registrada

---

## Phase 9: User Story 7 - Recibir notificaciones de todos los módulos (Priority: P3) — CU29

**Goal**: los orígenes de CU29 que faltaban generan notificaciones con el enlace correcto.

**Independent Test**: `NotificacionOrigenesTest` en verde; paso 13 de quickstart.md.

### Tests for User Story 7 ⚠️

- [X] T050 [P] [US7] Escribir `TEST/NotificacionOrigenesTest.java` (CU29) con HU7-1 a HU7-5 de spec.md:
  - convocatoria publicada notifica a socios con cuenta, no a los que no tienen cuenta, ni a inactivos ni a otra junta;
  - mensaje nuevo con enlace a la conversación;
  - cuota emitida (CU30) y multa aprobada (CU10) notifican al socio con cuenta, con enlace a `/mi-informacion/estado-cuenta`;
  - turno creado notifica al socio con enlace a `/mi-informacion/calendario`;
  - las notificaciones de recuperación y corrección siguen intactas;
  - contador de la campana en `/contadores`

### Implementation for User Story 7

- [X] T051 [US7] Notificar "Nueva cuota" en `JAVA/service/CuotaService.java` al emitir y "Nueva multa" en `JAVA/service/MultaService.java` al aprobar, a los socios con cuenta activa (research.md R7); mantener verdes `TEST/CuotaTest.java` y `TEST/MultaTest.java`, incluida la prueba de rendimiento de 200 multas
- [X] T052 [US7] Cambiar en `JAVA/service/TurnoService.java` el enlace de la notificación del socio a `/mi-informacion/calendario` y ajustar `TEST/TurnoTest.java` si comprueba el enlace
- [X] T053 [US7] Revisar que las notificaciones de convocatorias (T018) y mensajes (T030) usan los títulos de contracts/mensajes.md
- [X] T054 [US7] Ejecutar `TEST/NotificacionOrigenesTest.java` y las pruebas de 003 y 004 afectadas hasta que pasen

**Checkpoint**: la campana cubre todos los orígenes de CU29

---

## Phase 10: Polish & Cross-Cutting Concerns

- [X] T055 Dejar `JAVA/rest/EnConstruccionController.java` sin rutas de módulos (solo `/en-construccion`) y comprobar que el menú de cada rol no lleva a la página en construcción
- [X] T056 Ampliar `JAVA/config/DatosPrueba.java` según research.md R11 sin duplicar en reinicios, y actualizar `TEST/DatosPruebaTest.java` y `README.md` (sección 005)
- [X] T057 Prueba de tiempo en `TEST/MiInformacionTest.java`: Mi Panel con 50 pagos y 50 obligaciones responde en menos de 5 s (SC-001)
- [X] T058 Ejecutar `.\mvnw.cmd test` completo (001 a 005) y dejar todo en verde
- [X] T059 [P] Revisar que ningún archivo bajo `src/` tenga comentarios (Principio VI)
- [X] T060 [P] Revisar textos en español y ausencia de desplazamiento horizontal a 360 px en las pantallas nuevas (FR-032, SC-007)
- [X] T061 Ejecutar la validación manual de `specs/005-gestion-comunicaciones/quickstart.md` (pasos 1 a 15) y restaurar `data/` y `uploads/` al terminar
- [X] T062 Verificar trazabilidad: cada FR de spec.md cubierto por una tarea y una prueba con su CUxx (Principio VIII)

---

## Dependencies & Execution Order

- **Setup (1)** → **Foundational (2)** → historias
- **US1 (3)** CU17: tras Foundational
- **US2 (4)** CU15: tras Foundational
- **US3 (5)** CU32: tras Foundational (usa Convocatoria y Acta de Foundational y `Fabrica`); T026 necesita US1
- **US4 (6)** CU18: tras Foundational
- **US5 (7)** CU16: tras Foundational; T042 vuelve a ejecutar `AnuncioTest` (US3)
- **US6 (8)** CU19: tras Foundational
- **US7 (9)** CU29: tras US2 y US4 (revisa sus notificaciones)
- **Polish (10)**: tras todas

### Within Each User Story

- Prueba primero (debe fallar) → entidad y repositorio → formulario y datos → servicio →
  controlador y vistas → ejecución de pruebas

### Parallel Opportunities

- Setup: T001 y T002; Foundational: T003 a T007
- Tras Foundational, cinco líneas en paralelo:
  - US1 → US3 (pantallas del socio);
  - US2 (convocatorias);
  - US4 (mensajes);
  - US5 (actas);
  - US6 (comunicaciones).
- Dentro de cada historia, las tareas [P] de prueba, entidad y formulario

---

## Parallel Example: User Story 4

```text
Task: "Escribir TEST/MensajeTest.java (CU18)"
Task: "Crear JAVA/model/Conversacion.java, Mensaje.java y sus repositorios"
```

---

## Implementation Strategy

### MVP First

1. Setup + Foundational
2. US1 (CU17) → panel del socio
3. US2 (CU15) y US3 (CU32) → convocatorias que el socio recibe y consulta

### Incremental Delivery

1. MVP (US1 a US3)
2. US4 → mensajería
3. US5 y US6 → actas y correspondencia externa
4. US7 → notificaciones completas
5. Polish → sin módulos en construcción, datos de prueba y puertas de calidad

---

## Notes

- [P] = archivos distintos, sin dependencias pendientes
- [USx] y el CUxx en la descripción dan la trazabilidad exigida por la constitución
- Detenerse en cada checkpoint para validar la historia

---

## Phase 11: Convergence

- [X] T063 Agregar `Socio.isNotificable()` (cuenta, activo y estado ACTIVO) y usarlo en todas las notificaciones al socio: cuota individual (`JAVA/service/CuotaService.java`), multa (`JAVA/service/MultaService.java`), mensaje de la directiva (`JAVA/service/MensajeService.java`), turno (`JAVA/service/TurnoService.java`) y convocatoria (`JAVA/service/ConvocatoriaService.java`), con prueba en `TEST/NotificacionOrigenesTest.java` de un socio INACTIVO con cuenta que no recibe nada, per spec.md Edge Cases (socio inactivo no recibe notificaciones) (partial)
- [X] T064 Rechazar el inicio de sesión de un socio INACTIVO aunque su cuenta esté activa (mismo mensaje de cuenta desactivada de 001) en `JAVA/config/SeguridadConfig.java` y cerrar la sesión de un socio que pase a INACTIVO en `JAVA/config/EstadoCuentaFilter.java`, con prueba en `TEST/MiInformacionTest.java`, per spec.md Edge Cases (socio inactivo no puede iniciar sesión) (missing)
