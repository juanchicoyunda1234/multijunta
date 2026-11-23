# Contrato: uso de IAsistencia y evento AsistenciaCorregida (004)

El módulo financiero depende solo de la interfaz `IAsistencia` del módulo administrativo
(diagrama de componentes) y de un evento de aplicación. No usa `AsistenciaRepository` ni la
entidad `Asistencia`.

## IAsistencia (paquete `service`)

| Método | Devuelve | Uso en 004 |
|---|---|---|
| `inasistenciasDeEventosCerrados(Long juntaId)` (003) | `List<Inasistencia>` con estado AUSENTE de eventos con asistencia cerrada, más recientes primero | tabla de sincronización de multas (CU10 FR-011) |
| `inasistenciasDelEvento(Long juntaId, Long eventoId)` (003) | `List<Inasistencia>` de un evento | sin cambios |
| `eventosSinCerrar(Long juntaId)` (**nuevo**) | `List<EventoSinCerrar>`: Asambleas y Mingas PROGRAMADAS de hoy o anteriores sin asistencia cerrada | bloque "Acta de evento no oficializada por Secretaría" (CU10 FR-014) |

`record EventoSinCerrar(Long eventoId, TipoEvento tipoEvento, LocalDate fecha, String lugar)`.

Reglas: devuelve solo datos de `juntaId`; JUSTIFICADO y PRESENTE nunca aparecen como
inasistencia.

## AsistenciaCorregida (paquete `service`)

`record AsistenciaCorregida(Long juntaId, Long asistenciaId, EstadoAsistencia estadoAnterior,
EstadoAsistencia estadoNuevo)`

- Lo publica `CorreccionAsistenciaService` al **aprobar** una corrección, dentro de la misma
  transacción.
- Lo escucha `MultaService` (`@EventListener`):
  - estado anterior ≠ AUSENTE o sin multa no anulada → no hace nada;
  - multa con `saldoPendiente = monto` (sin pagos vigentes) → ANULADA con motivo "Asistencia corregida" y
    saldo 0;
  - multa con `saldoPendiente < monto` (pagos vigentes) → notificación "Multa pagada en asistencia corregida" a cada
    TESORERO activo de la junta, con enlace `/multas`.
- Un cambio a AUSENTE no requiere acción: la inasistencia aparece en la siguiente
  sincronización.
