# Implementation Plan: Módulo Gestión de Comunicaciones

**Branch**: `005-gestion-comunicaciones` | **Date**: 2026-11-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/005-gestion-comunicaciones/spec.md`

## Summary

Construir el último módulo sobre 001–004.

- **Para el socio:**
  - "Mi Panel" (CU17): resumen, estado de cuenta imprimible, predio en el mapa y calendario de
    turnos;
  - "Anuncios" y "Repositorio" (CU32).
- **Para el secretario:**
  - convocatorias con publicación inmediata o programada por una tarea `@Scheduled`, y oferta
    desde Eventos (CU15);
  - actas en PDF con visibilidad para socios (CU16);
  - comunicaciones externas con número de ingreso que no se reutiliza (CU19).
- **Para todos:** mensajería entre socio y directiva y dentro de la directiva, con imagen y
  contadores actualizados cada 15 segundos sin WebSocket (CU18).
- **Notificaciones (CU29):** se agregan las de convocatorias, mensajes, multas y cuotas.
- **Cierre:** ya no queda ningún módulo "en construcción".

## Technical Context

**Language/Version**: Java 17 (compila con JDK 21)

**Primary Dependencies**: Spring Boot 3.5.16 (web, data-jpa, security, validation, thymeleaf),
Bootstrap 5.3 por CDN, Leaflet 1.9.4 (fragmento de 003), OpenPDF y POI (004), H2. Sin
dependencias Maven nuevas; se habilita `@EnableScheduling` de Spring.

**Storage**: H2 en archivo. Tablas nuevas creadas por JPA (`ddl-auto=update`): `convocatoria`,
`acta`, `comunicacion_externa`, `secuencia_ingreso`, `conversacion` y `mensaje`. Archivos en
`./uploads/actas`, `./uploads/comunicaciones` y `./uploads/mensajes`.

**Testing**: JUnit 5, Spring Boot Test, MockMvc, spring-security-test y H2 en memoria. El
programador se desactiva en el perfil `test` y su método se invoca directamente.

**Target Platform**: servidor único JRE 17+; navegador de computador y celular

**Project Type**: aplicación web monolítica con vistas en el servidor

**Performance Goals**:
- Mi Panel en menos de 5 s (SC-001);
- publicación programada con a lo sumo 1 minuto de retraso (SC-003);
- mensajes visibles en el contador en menos de 20 s (SC-004).

**Constraints**:
- sin comentarios en el código;
- textos en español;
- responsive desde 360 px;
- archivos de hasta 5 MB;
- sin WebSocket.

**Scale/Scope**: unas 13 pantallas: 5 del socio, 2 de convocatorias, actas, comunicaciones,
mensajes y los fragmentos de burbujas y pestañas. Son 7 servicios nuevos y 4 que cambian.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento en este plan | Estado |
|---|---|---|
| I. Especificación como fuente de verdad | Cada vista y regla sale de un FR de `spec.md`; el mockup del módulo 4 guía las vistas | ✅ |
| II. Aislamiento por junta | Entidades nuevas con su junta; `Mensaje` la hereda de `Conversacion`; el socio se toma siempre de la sesión (R6); prueba A/B en cada historia | ✅ |
| III. Permisos por rol | Filas existentes de `MatrizPermisos`, más `/contadores` exacta para todos con sesión; el servicio de mensajes verifica participante y regla socio–directiva (FR-017) | ✅ |
| IV. Español y responsive | Vista de mensajes apilada en celular (contactos o conversación); pestañas del socio desplazables | ✅ |
| V. Simplicidad | Sin dependencias nuevas; `@Scheduled` como dicta tech.md; sondeo cada 15 s en vez de WebSocket, como pide CU18 | ✅ |
| VI. Código sin comentarios | Revisión en la fase de pulido | ✅ |
| VII. Verificación contra criterios | Una clase de prueba por CU con todos sus escenarios; quickstart manual de 15 pasos | ✅ |
| VIII. Trazabilidad CUxx | Contratos, pruebas y tareas citan el CU | ✅ |

**Re-evaluación tras el diseño (Fase 1)**: sin violaciones.
- El sondeo de `/contadores` devuelve solo números del usuario en sesión.
- La única ruta nueva de la matriz es `/contadores`, de acceso universal con sesión y sin datos
  de otros usuarios.
- La adaptación de `Mensaje` respecto del modelo de dominio queda documentada (R2).

## Project Structure

### Documentation (this feature)

```text
specs/005-gestion-comunicaciones/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── rutas-ui.md
│   └── mensajes.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/java/ec/uta/multijunta/
├── model/
│   ├── TipoConvocatoria.java, PrioridadConvocatoria.java, EstadoConvocatoria.java, TipoDocumentoExterno.java
│   ├── Convocatoria.java, Acta.java, ComunicacionExterna.java, SecuenciaIngreso.java
│   └── Conversacion.java, Mensaje.java
├── repository/
│   ├── ConvocatoriaRepository.java, ActaRepository.java, ComunicacionExternaRepository.java
│   ├── SecuenciaIngresoRepository.java, ConversacionRepository.java, MensajeRepository.java
│   └── TurnoRepository.java, PagoRepository.java   (+ consultas para el socio)
├── service/
│   ├── MiInformacionService.java     (CU17)
│   ├── AnuncioService.java           (CU32)
│   ├── ConvocatoriaService.java      (CU15, programador)
│   ├── ActaService.java              (CU16)
│   ├── ComunicacionExternaService.java (CU19)
│   ├── MensajeService.java           (CU18 y contadores)
│   ├── Datos*.java                   (DatosConvocatoria, DatosActa, DatosComunicacion)
│   ├── ArchivoService.java           (cambia: validarPdf, carpetas actas, comunicaciones, mensajes)
│   ├── CuotaService.java, MultaService.java, TurnoService.java (cambian: notificaciones CU29)
│   └── PanelService.java             (cambia: mensajes no leídos)
├── rest/
│   ├── MiInformacionController.java (/mi-informacion), AnuncioController.java (/anuncios)
│   ├── ConvocatoriaController.java (/convocatorias), ActaController.java (/actas)
│   ├── ComunicacionExternaController.java (/comunicaciones), MensajeController.java (/mensajes)
│   ├── ContadorController.java (/contadores)
│   ├── InicioController.java         (cambia: /mi-informacion pasa a MiInformacionController)
│   ├── EventoController.java         (cambia: oferta de convocatoria)
│   ├── EnConstruccionController.java (cambia: sin módulos pendientes)
│   ├── VistaComunAdvice.java         (cambia: mensajes no leídos, archivos grandes)
│   └── formulario/
│       ├── ConvocatoriaFormulario.java, ActaFormulario.java, ComunicacionFormulario.java
└── config/
    ├── MatrizPermisos.java           (cambia: /contadores)
    ├── ProgramadorConfig.java        (@EnableScheduling condicionado por propiedad)
    └── DatosPrueba.java              (cambia: datos de comunicaciones, R11)
src/main/resources/
├── application.properties            (+ multijunta.programador.habilitado=true)
├── messages.properties               (+ contracts/mensajes.md)
├── static/css/app.css                (+ burbujas, vista dividida, deuda en verde y rojo)
└── templates/
    ├── socio/resumen.html, socio/estado-cuenta.html, socio/predio.html, socio/calendario.html
    ├── socio/anuncios.html, socio/repositorio.html, fragments/socio.html (pestañas)
    ├── convocatorias/lista.html, convocatorias/formulario.html
    ├── actas/lista.html, comunicaciones/lista.html
    ├── mensajes/vista.html, mensajes/burbujas.html
    ├── fragments/layout.html          (cambia: ícono Mensajes con contador y sondeo cada 15 s)
    ├── eventos/calendario.html        (cambia: "Crear convocatoria")
    └── inicio/socio.html              (se elimina; la reemplaza socio/resumen.html)
src/test/java/ec/uta/multijunta/
├── MiInformacionTest.java (CU17), ConvocatoriaTest.java (CU15), AnuncioTest.java (CU32)
├── MensajeTest.java (CU18), ActaTest.java (CU16), ComunicacionExternaTest.java (CU19)
├── NotificacionOrigenesTest.java (CU29)
└── PanelTest.java, PermisosPorRolTest.java, DatosPruebaTest.java, Fabrica.java (cambian)
src/test/resources/
└── application-test.properties       (+ multijunta.programador.habilitado=false)
```

**Structure Decision**: mismo proyecto y capas de 001–004, con formularios en `rest.formulario`
que se convierten a records `Datos*` de `service`. Las rutas del socio no reciben ids de socio:
el socio sale de la sesión.

## Complexity Tracking

Sin violaciones a la constitución; no se requiere justificación.
