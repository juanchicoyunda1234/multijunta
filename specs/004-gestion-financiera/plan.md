# Implementation Plan: Módulo Gestión Financiera

**Branch**: `004-gestion-financiera` | **Date**: 2026-11-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-gestion-financiera/spec.md`

## Summary

Construir el módulo financiero de la junta sobre 001–003:
- cuotas y obligaciones con vista previa (CU30);
- cobranzas con abono parcial, número de transacción y recibo imprimible (CU09);
- multas sincronizadas desde las inasistencias de eventos cerrados a través de `IAsistencia`
  (CU10);
- egresos con comprobante y saldo actual (CU11);
- reverso exacto de pagos (CU31);
- inventario con kardex (CU13);
- activos fijos con depreciación lineal (CU14);
- reportes Balance General y Socios Morosos en PDF (OpenPDF) y Excel (Apache POI) (CU12).

Las correcciones de asistencia aprobadas (CU28) anulan o señalan multas mediante un evento de
aplicación, sin que el módulo administrativo dependa del financiero. Se completan las tres
tarjetas financieras del panel.

## Technical Context

**Language/Version**: Java 17 (compila con JDK 21)

**Primary Dependencies**: Spring Boot 3.5.16 (web, data-jpa, security, validation, thymeleaf),
Bootstrap 5.3 por CDN, H2; **nuevas**: OpenPDF 2.0.5 y Apache POI `poi-ooxml` 5.5.1 (tech.md,
research R1)

**Storage**: H2 en archivo; tablas nuevas creadas por JPA (`ddl-auto=update`):
- `cuota`, `multa`, `pago`, `aplicacion_pago`, `reverso_pago`;
- `egreso`;
- `item_inventario`, `movimiento_inventario`;
- `activo_fijo`.

Archivos en `./uploads/egresos` y `./uploads/activos`.

**Testing**: JUnit 5, Spring Boot Test, MockMvc, spring-security-test, H2 en memoria; los
reportes se verifican leyendo el PDF y el Excel generados con las mismas librerías

**Target Platform**: servidor único JRE 17+; navegador de computador y celular

**Project Type**: aplicación web monolítica con vistas en el servidor

**Performance Goals**:
- aprobar 200 multas en menos de 3 s (SC-002);
- generar un reporte mensual de 500 movimientos en menos de 10 s (SC-006);
- páginas en menos de 1 s.

**Constraints**:
- sin comentarios en el código;
- textos en español;
- responsive desde 360 px;
- montos `BigDecimal` con 2 decimales;
- archivos hasta 5 MB;
- pagos inmutables (solo reverso).

**Scale/Scope**: unas 14 pantallas (cuotas, cobranzas, cobro, detalle de pago, recibo, historial,
multas, sincronización, egresos, reportes, inventario, kardex, activos) y 8 servicios

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento en este plan | Estado |
|---|---|---|
| I. Especificación como fuente de verdad | Cada servicio, vista y regla sale de un FR de `spec.md`; el mockup del módulo 3 guía las vistas | ✅ |
| II. Aislamiento por junta | Todas las entidades nuevas guardan su junta (`AplicacionPago` y `MovimientoInventario` la heredan); repositorios `...AndJuntaId`; prueba A/B en cada historia | ✅ |
| III. Permisos por rol | Filas existentes de `MatrizPermisos` (`/pagos`, `/multas`, `/egresos`, `/reportes`, `/inventario`, `/activos`, `/cuotas`); el reverso es un POST bajo `/pagos`, solo TESORERO | ✅ |
| IV. Español y responsive | Bootstrap; tablas con `table-responsive`; recibo con estilos de impresión | ✅ |
| V. Simplicidad | Las dos dependencias nuevas son las que fija tech.md; sin JavaScript para confirmaciones (R9); saldos calculados, no duplicados (R7) | ✅ |
| VI. Código sin comentarios | Revisión en la fase de pulido | ✅ |
| VII. Verificación contra criterios | Una clase de prueba por CU con todos sus escenarios; quickstart manual de 16 pasos | ✅ |
| VIII. Trazabilidad CUxx | Contratos, pruebas y tareas citan el CU | ✅ |

**Re-evaluación tras el diseño (Fase 1)**: sin violaciones.
- El módulo financiero usa solo `IAsistencia` (más un método nuevo, `eventosSinCerrar`) y escucha
  `AsistenciaCorregida`. Ambos viven en `service`, así que se mantiene `rest → service →
  repository → model` y el diagrama de componentes ("Gestión Financiera usa IAsistencia").
- OpenPDF se fija en 2.0.5 porque las versiones posteriores exigen Java 21 (R1).

## Project Structure

### Documentation (this feature)

```text
specs/004-gestion-financiera/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── rutas-ui.md
│   ├── mensajes.md
│   └── iasistencia.md
└── tasks.md
```

### Source Code (repository root)

```text
pom.xml                               (+ openpdf 2.0.5, poi-ooxml 5.5.1)
src/main/java/ec/uta/multijunta/
├── model/
│   ├── EstadoObligacion.java, MetodoPago.java, CategoriaEgreso.java
│   ├── EstadoItem.java, TipoMovimiento.java, CategoriaActivo.java, EstadoActivo.java
│   ├── Cuota.java, Multa.java, Pago.java, AplicacionPago.java, ReversoPago.java
│   ├── Egreso.java, ItemInventario.java, MovimientoInventario.java, ActivoFijo.java
├── repository/
│   ├── CuotaRepository.java, MultaRepository.java, PagoRepository.java
│   ├── AplicacionPagoRepository.java, ReversoPagoRepository.java, EgresoRepository.java
│   ├── ItemInventarioRepository.java, MovimientoInventarioRepository.java, ActivoFijoRepository.java
│   └── UsuarioRepository.java       (sin cambios: findByJuntaIdAndRolAndActivoTrue para tesoreros)
├── service/
│   ├── CuotaService.java             (CU30)
│   ├── CobranzaService.java          (CU09: deuda, cobro, aplicación, número de transacción)
│   ├── ReversoService.java           (CU31)
│   ├── MultaService.java             (CU10; escucha AsistenciaCorregida)
│   ├── EgresoService.java            (CU11)
│   ├── FinanzasService.java          (saldo actual, deuda por cobrar)
│   ├── InventarioService.java        (CU13)
│   ├── ActivoFijoService.java        (CU14)
│   ├── ReporteService.java, ReportePdf.java, ReporteExcel.java (CU12)
│   ├── EventoSinCerrar.java, AsistenciaCorregida.java (records)
│   ├── Datos*.java                   (DatosCuota, DatosCobro, DatosEgreso, DatosItem, DatosMovimiento, DatosActivo, DatosReporte)
│   ├── IAsistencia.java, AsistenciaService.java   (cambia: eventosSinCerrar)
│   ├── CorreccionAsistenciaService.java (cambia: publica AsistenciaCorregida al aprobar)
│   ├── ArchivoService.java           (cambia: validarDocumento, carpetas egresos y activos)
│   └── PanelService.java             (cambia: saldo, deuda y stock bajo reales)
├── rest/
│   ├── CuotaController.java (/cuotas), CobranzaController.java (/pagos)
│   ├── MultaController.java (/multas), EgresoController.java (/egresos)
│   ├── ReporteController.java (/reportes), InventarioController.java (/inventario)
│   ├── ActivoFijoController.java (/activos)
│   ├── EnConstruccionController.java (cambia: quita rutas construidas)
│   ├── VistaComunAdvice.java         (cambia: archivo grande en /egresos y /activos)
│   └── formulario/
│       ├── CuotaFormulario.java, CobroFormulario.java, EgresoFormulario.java
│       └── ItemFormulario.java, MovimientoFormulario.java, ActivoFormulario.java, ReporteFormulario.java
└── config/
    └── DatosPrueba.java              (cambia: datos financieros de prueba, R13)
src/main/resources/
├── messages.properties               (+ contracts/mensajes.md)
├── static/css/app.css                (+ recibo imprimible, fila crítica, montos)
└── templates/
    ├── cuotas/lista.html, cuotas/emitir.html
    ├── pagos/cobranzas.html, pagos/cobro.html, pagos/detalle.html, pagos/recibo.html, pagos/historial.html
    ├── multas/lista.html, multas/sincronizar.html
    ├── egresos/lista.html
    ├── reportes/formulario.html
    ├── inventario/lista.html, inventario/kardex.html
    └── activos/lista.html
src/test/java/ec/uta/multijunta/
├── CuotaTest.java (CU30), CobranzaTest.java (CU09), ReversoTest.java (CU31)
├── MultaTest.java (CU10 y efecto de CU28), EgresoTest.java (CU11)
├── InventarioTest.java (CU13), ActivoFijoTest.java (CU14), ReporteTest.java (CU12)
└── PanelTest.java, PermisosPorRolTest.java, DatosPruebaTest.java, Fabrica.java (cambian)
```

**Structure Decision**: mismo proyecto y capas de 001–003, con el patrón de formularios en
`rest.formulario` que se convierten a records `Datos*` en `service`. La lógica de PDF y Excel
vive en `service` (`ReportePdf`, `ReporteExcel`) y el controlador solo devuelve los bytes.

## Complexity Tracking

Sin violaciones a la constitución; no se requiere justificación.
