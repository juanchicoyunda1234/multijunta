# Quickstart: validar el módulo Gestión Administrativa

## Ejecutar

```powershell
.\mvnw.cmd spring-boot:run
```

```powershell
.\mvnw.cmd test
```

Resultado esperado: todo en verde, incluidas `NominaTest`, `EventoTest`, `AsistenciaTest`,
`RecursoTest`, `TurnoTest`, `PredioTest` y `CorreccionAsistenciaTest`. Para partir de los datos
de prueba, detener la app y borrar `data/` y `uploads/`. El mapa necesita internet.

## Validación manual

Rutas en [contracts/rutas-ui.md](contracts/rutas-ui.md); mensajes en
[contracts/mensajes.md](contracts/mensajes.md); interfaz en
[contracts/iasistencia.md](contracts/iasistencia.md).

| # | Pasos | Resultado esperado | Spec |
|---|---|---|---|
| 1 | Como `secretaria.sanpedro`, Nómina → "Añadir Nuevo Socio", escribir cédula `12345` | Botón deshabilitado con texto de ayuda | HU1-4 |
| 2 | Completar con cédula válida nueva y guardar | El socio aparece primero con código `S-…` y "Socio registrado" | HU1-3 |
| 3 | Buscar por cédula, editar celular, pasarlo a INACTIVO | Cambios visibles | HU1-7, HU1-8 |
| 4 | Eventos → "Nuevo Evento" (Minga, mañana); editarlo y cancelarlo | Tarjeta "Cancelado", sin "Tomar Asistencia" | HU2-1..3 |
| 5 | Asistencia → Asamblea de ayer (datos de prueba) → marcar 1 Presente, 1 Justificado → cerrar | Resumen con porcentaje; registro bloqueado | HU3-1..4 |
| 6 | En el detalle, "Solicitar corrección" de un Ausente a Justificado con motivo | Solicitud enviada | HU7-1 |
| 7 | Como `admin.sanpedro`: campana → aprobar la corrección | Estado cambiado; historial con auditoría | HU7-3 |
| 8 | Como secretaria, Recursos: crear "Compuerta #9", intentar "compuerta #9" | Duplicado rechazado | HU4-2, HU4-3 |
| 9 | Calendario: agendar dos turnos del mismo recurso que se crucen | El segundo: "Conflicto de recursos detectado" | HU5-2, HU5-3 |
| 10 | Reprogramar un turno a otro día | Bloque movido; socio con cuenta del sector (`1800000101`) ve la notificación | HU5-4, HU5-5 |
| 11 | Perfil de un socio → "Asignar Predio" sin pin; luego con pin y foto PNG | Rechazo sin pin; luego predio con mapa y foto | HU6-2, HU6-5 |
| 12 | Subir foto de más de 5 MB | "El archivo excede el límite permitido" | HU6-4 |
| 13 | Como `admin.sanpedro`, panel | "Próximos eventos y turnos" y "Asistencias pendientes de cerrar" con números | FR-032 |
| 14 | Como `tesorero.sanpedro`, abrir Nómina y Asistencia | Solo lectura, sin botones de cambio | HU1-9, HU3-7 |
| 15 | Abrir cada pantalla a 360 px | Sin desplazamiento horizontal | SC-006 |
