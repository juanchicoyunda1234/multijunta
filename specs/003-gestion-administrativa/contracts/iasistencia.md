# Contrato: interfaz IAsistencia (CU06 → módulo financiero)

Del diagrama de componentes: **Gestión Administrativa expone IAsistencia; Gestión Financiera
la usa.** Es una interfaz Java de la capa `service`, no una API HTTP.

```text
paquete: ec.uta.multijunta.service

interface IAsistencia
    List<Inasistencia> inasistenciasDeEventosCerrados(Long juntaId)
    List<Inasistencia> inasistenciasDelEvento(Long juntaId, Long eventoId)

record Inasistencia(Long asistenciaId, Long eventoId, TipoEvento tipoEvento, LocalDate fecha,
                    Long socioId, String cedula, String nombreCompleto)
```

## Garantías

1. Solo incluye eventos con asistencia cerrada de la junta indicada; un `eventoId` de otra junta
   o sin asistencia cerrada devuelve lista vacía.
2. Solo incluye asistencias con estado AUSENTE; JUSTIFICADO y PRESENTE nunca aparecen (CU06
   alterno).
3. Refleja el estado actual: si CU28 aprueba pasar un AUSENTE a JUSTIFICADO, deja de aparecer.
4. Orden: fecha del evento ascendente y luego apellidos del socio.
5. Solo lectura: no modifica datos; transacción de solo lectura.
6. No depende de la sesión: el consumidor pasa la junta (el módulo financiero ya trabaja sobre
   la junta del usuario autenticado).

## Uso previsto (CU10, módulo financiero)

Generar multas por inasistencia con el monto de `Junta.montoMultaInasistencia`, enlazando la
multa con `asistenciaId` (relación "Asistencia genera Multa" del modelo de dominio).

## Verificación

`AsistenciaTest` comprueba las garantías 1 a 4 con dos juntas, un evento abierto, un evento
cerrado y una corrección aprobada.
