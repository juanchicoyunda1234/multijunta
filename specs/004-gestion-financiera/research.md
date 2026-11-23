# Research: Módulo Gestión Financiera

Decisiones técnicas de la funcionalidad 004. No quedan puntos "NEEDS CLARIFICATION".

## R1. Librerías de PDF y Excel (CU12)

- **Decision**: OpenPDF `com.github.librepdf:openpdf:2.0.5` para PDF y Apache POI
  `org.apache.poi:poi-ooxml:5.5.1` para Excel (`.xlsx`).
- **Rationale**: `docs/contexto/tech.md` fija OpenPDF y Apache POI. OpenPDF 2.0.5 es la última
  versión compilada para Java 17 (2.2.x, 2.4.x y 3.x exigen Java 21, verificado en los POM de
  Maven Central); el proyecto declara `java.version` 17. POI 5.5.1 es la última 5.x (base Java
  11), sin el salto de versión mayor de la 6.0.
- **Alternatives considered**: OpenPDF 3.0.6 (requiere Java 21, rompe el requisito "JDK 17 o
  superior" del README); generar CSV en vez de Excel (la spec pide Excel); HTML a PDF con
  openhtmltopdf (otra dependencia no prevista en tech.md).

## R2. Número de transacción

- **Decision**: correlativo por junta con formato `TX-000001`, compartido por pagos y reversos;
  se calcula como `count(pagos de la junta) + count(reversos de la junta) + 1` dentro de la
  transacción, con índice único `(junta_id, numero)` en cada tabla.
- **Rationale**: simple, legible en recibos y reportes, y no requiere tabla de secuencias.
- **Alternatives considered**: UUID (ilegible en un recibo); secuencia global (los números
  saltan entre juntas y revelan volumen de otras juntas).

## R3. Aplicación de pagos y reversos

- **Decision**: entidad `AplicacionPago` (pago, multa o cuota, monto). El cobro ordena las
  obligaciones marcadas por fecha de vencimiento (o emisión si no tiene) y aplica el monto de la
  más antigua a la más reciente. El reverso recorre las aplicaciones del pago y suma cada monto
  al saldo de su obligación; el estado se recalcula (`saldo == monto` → PENDIENTE, `0 < saldo <
  monto` → PARCIAL, `saldo == 0` → PAGADA).
- **Rationale**: el modelo de dominio exige guardar "cuánto aplicó a cada una para poder
  reversarlo"; con el detalle el recibo y el reverso son exactos (SC-004).
- **Alternatives considered**: guardar solo el total y recalcular por diferencia (imposible
  reversar un pago intermedio con exactitud).

## R4. Concurrencia en cobros

- **Decision**: `@Version` en `Multa` y `Cuota`; además el servicio rechaza el cobro si el monto
  supera la suma de saldos actuales de lo marcado. Una `ObjectOptimisticLockingFailureException`
  se traduce a "Las obligaciones cambiaron; vuelva a cargar el cobro".
- **Rationale**: cubre el caso borde de dos tesoreros cobrando a la vez sin bloqueos explícitos.
- **Alternatives considered**: bloqueo pesimista (innecesario para el volumen de una junta).

## R5. Multas a partir de `IAsistencia`

- **Decision**: `MultaService` usa solo `IAsistencia` (no `AsistenciaRepository` ni la entidad
  `Asistencia`). `Multa` guarda `asistenciaId` como columna `Long` con índice único (una multa por
  asistencia), más `eventoId`, `tipoEvento` y `fechaEvento` copiados para mostrar. Para el bloque
  "Acta de evento no oficializada por Secretaría" se agrega a `IAsistencia` el método
  `eventosSinCerrar(Long juntaId)` que devuelve el record `EventoSinCerrar(eventoId, tipoEvento,
  fecha, lugar)` (Asamblea o Minga PROGRAMADA de hoy o anterior sin asistencia cerrada).
- **Rationale**: respeta el diagrama de componentes ("Gestión Financiera usa IAsistencia") y el
  plan de 003; el índice único garantiza FR-013 incluso ante doble envío.
- **Alternatives considered**: relación JPA `Multa → Asistencia` (acopla el módulo financiero a
  las entidades administrativas).

## R6. Efecto de las correcciones de asistencia sobre multas (FR-015)

- **Decision**: `CorreccionAsistenciaService.resolver` publica, al aprobar, un evento de
  aplicación `AsistenciaCorregida(juntaId, asistenciaId, estadoAnterior, estadoNuevo)` con
  `ApplicationEventPublisher`. `MultaService` lo escucha con `@EventListener` (misma
  transacción): si el estado anterior era AUSENTE y existe multa no anulada para esa asistencia,
  la anula con motivo "Asistencia corregida" cuando su `saldoPendiente` es igual al monto (sin pagos vigentes, R3); si
  no, notifica a los TESORERO activos de la junta ("Multa pagada en asistencia corregida",
  enlace `/multas`).
- **Rationale**: evita que el módulo administrativo dependa del financiero (sin ciclo
  `service ↔ service`); el evento queda en `service` y no cambia capas.
- **Alternatives considered**: que `CorreccionAsistenciaService` llame a `MultaService`
  (dependencia inversa al diagrama de componentes).

## R7. Saldo actual, deuda y patrimonio

- **Decision**: valores calculados con consultas agregadas (`sum`) en cada pantalla:
  - Saldo actual = Σ pagos no reversados − Σ egresos.
  - Deuda de un socio = Σ saldo pendiente de multas y cuotas no ANULADAS.
  - Patrimonio = Σ valor en libros de activos no DADO_DE_BAJA; la depreciación se calcula en la
    entidad (`ActivoFijo.depreciacion(LocalDate hoy)`) por meses completos.
- **Rationale**: sin campos duplicados que desincronizar; volúmenes de una junta son pequeños.
- **Alternatives considered**: guardar saldos acumulados en `Junta` (riesgo de inconsistencia
  con reversos).

## R8. Archivos de egresos y activos

- **Decision**: `ArchivoService` agrega `validarDocumento(...)` que acepta PDF (`%PDF`), PNG y
  JPG por bytes mágicos, hasta 5 MB, y las subcarpetas `egresos` y `activos`. Se sirven por
  `/egresos/{id}/comprobante` y `/activos/{id}/archivo` solo a la junta dueña. El manejador de
  `MaxUploadSizeExceededException` de `VistaComunAdvice` redirige a `/egresos` o `/activos` según
  la ruta original.
- **Rationale**: reutiliza la validación y almacenamiento de 002/003 (`./uploads`, 5 MB).
- **Alternatives considered**: guardar los archivos en la base (crece el H2 sin necesidad).

## R9. Confirmaciones (abono parcial, fondos insuficientes, vista previa de cuotas)

- **Decision**: se resuelven en el servidor con un campo oculto de confirmación: el primer envío
  valida y, si falta confirmar, vuelve al formulario con el aviso y un botón "Confirmar" que
  reenvía con `confirmar=true`. La vista previa de cuotas es un `POST /cuotas/previa` que muestra
  el resumen sin guardar.
- **Rationale**: funciona sin JavaScript y es fácil de probar con MockMvc (constitución VII).
- **Alternatives considered**: `confirm()` del navegador (no comprobable del lado del servidor
  y depende de que el saldo calculado en el navegador esté al día).

## R10. Reportes

- **Decision**: `ReporteService` arma los datos (records `ReporteBalance` y `ReporteMorosos`);
  `ReportePdf` (OpenPDF) y `ReporteExcel` (POI) los convierten a bytes; el controlador responde
  con `Content-Disposition: attachment` y nombre `balance-AAAAMMDD-AAAAMMDD.pdf` o
  `morosos-....xlsx`. Si los datos vienen vacíos, no se genera archivo y se vuelve al formulario
  con "No existen registros en el rango seleccionado". La vista previa muestra el saldo del rango.
- **Rationale**: separa cálculo (probado con datos) de formato (probado leyendo el archivo con
  las mismas librerías en las pruebas).
- **Alternatives considered**: plantillas Thymeleaf convertidas a PDF (otra dependencia).

## R11. Códigos generados

- **Decision**: ítem de inventario `INV-0001`, activo `AF-AAAA-0001` (año de registro), ambos
  correlativos por junta, como los códigos `S-0001` y `P-0001` de 003.
- **Rationale**: coinciden con el mockup ("AF-2024-0089") y con la convención existente.

## R12. Pantallas que dejan de estar "en construcción" y pruebas existentes afectadas

- **Decision**: `EnConstruccionController` deja solo `/convocatorias`, `/actas`,
  `/comunicaciones`, `/anuncios` y `/mensajes`. `PermisosPorRolTest.moduloEnConstruccion` pasa a
  usar `/convocatorias` con SECRETARIO; `soloLecturaNoModifica` sigue con POST a `/pagos` como
  ADMIN_JUNTA (403). La tarjeta "Deuda por cobrar" enlaza a `/pagos` (donde está la deuda por
  socio) y `PanelTest.proximamenteYEnlaces` se ajusta al nuevo enlace y a que solo "Mensajes no
  leídos" sigue "Disponible próximamente".
- **Rationale**: mismo patrón aplicado en 003 cuando `/socios` y `/predios` dejaron de estar en
  construcción.

## R13. Datos de prueba

- **Decision**: por cada junta activa, `DatosPrueba` agrega:
  - una Minga de hace 10 días con asistencia cerrada (un socio AUSENTE y uno JUSTIFICADO) para
    sincronizar multas;
  - la cuota "Tarifa anual 2026" a los socios activos, con un pago parcial;
  - dos egresos (uno sin factura);
  - tres ítems de inventario (uno en stock crítico) con movimientos;
  - dos activos fijos.
- **Rationale**: permite probar a mano cada historia desde el primer arranque (tech.md: datos de
  prueba al arrancar).
