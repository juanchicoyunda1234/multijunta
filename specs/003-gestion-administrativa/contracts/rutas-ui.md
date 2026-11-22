# Contrato: rutas de la interfaz (003)

Las rutas de 001 y 002 siguen vigentes. Respuestas HTML en español; los POST llevan CSRF. Los
permisos por rol salen de las filas ya existentes de
`specs/001-base-acceso-sesion/contracts/matriz-permisos.md`: los GET los ven los roles C y L;
los POST solo los roles C. Un identificador de otra junta responde 404.

| Método y ruta | Comportamiento | CU / FR |
|---|---|---|
| GET `/socios?q=` | padrón con búsqueda por cédula o nombre, más recientes primero | CU05 FR-004, FR-005 |
| GET `/socios/nuevo` | formulario en dos tarjetas | CU05 FR-001 |
| POST `/socios` | registra; éxito → `/socios` con "Socio registrado"; error → formulario | CU05 FR-001..003 |
| GET `/socios/{id}` | perfil: datos, estado, predios con mapa (predios solo para roles con acceso a `/predios`) | CU05, CU07 FR-018 |
| GET `/socios/{id}/editar` y POST `/socios/{id}` | edita nombres, apellidos, celular, sector | CU05 FR-004 |
| POST `/socios/{id}/estado` | `estado=ACTIVO|INACTIVO` | CU05 FR-004 |
| GET `/eventos?mes=AAAA-MM` | calendario de actividades del mes con tarjetas | CU26 FR-009 |
| POST `/eventos` | crea | CU26 FR-007 |
| POST `/eventos/{id}` | edita (rechaza si asistencia cerrada) | CU26 FR-008 |
| POST `/eventos/{id}/cancelar` | cancela (rechaza si asistencia cerrada) | CU26 FR-008 |
| GET `/asistencia` | eventos con asistencia: pendientes de tomar y cerrados con resumen | CU06 |
| GET `/asistencia/eventos/{id}` | si el evento admite toma y el rol es SECRETARIO: formulario de toma; si está cerrado: detalle con resumen, estados y botón "Solicitar corrección" (SECRETARIO) | CU06, CU28 |
| POST `/asistencia/eventos/{id}/cerrar` | parámetros `estado[socioId]`; cierra y redirige al detalle | CU06 FR-012, FR-013 |
| GET `/asistencia/correcciones` | pendientes (con Aprobar/Rechazar para ADMIN_JUNTA) e historial | CU28 FR-027, FR-028 |
| POST `/asistencia/correcciones` | `asistenciaId`, `estadoNuevo`, `motivo`; solo SECRETARIO, si no 403 | CU28 FR-026 |
| POST `/asistencia/correcciones/{id}/aprobar` y `/rechazar` | solo ADMIN_JUNTA, si no 403 | CU28 FR-027 |
| GET `/recursos` | lista y formulario | CU27 FR-019 |
| POST `/recursos` y POST `/recursos/{id}` | crea y edita | CU27 FR-019 |
| POST `/recursos/{id}/desactivar` y `/reactivar` | cambia estado | CU27 FR-020 |
| GET `/turnos?semana=AAAA-MM-DD` | semana lunes–viernes que contiene la fecha (por defecto la actual) | CU08 FR-021 |
| POST `/turnos` | agenda; conflicto → "Conflicto de recursos detectado" con el modal abierto | CU08 FR-022, FR-023 |
| POST `/turnos/{id}` | reprograma fecha y horas | CU08 FR-024 |
| GET `/predios/nuevo?socioId=` | formulario con mapa | CU07 FR-015 |
| POST `/predios` (multipart) | guarda; éxito → `/socios/{socioId}` | CU07 FR-015..017 |
| GET `/predios/{id}/editar` y POST `/predios/{id}` (multipart) | edita y agrega foto | CU07 FR-018 |
| GET `/predios/{id}/foto` | imagen del predio de la junta; si no hay, 404 | CU07 FR-018 |

## Cambios a rutas existentes

- `EnConstruccionController` deja de atender `/socios`, `/asistencia`,
  `/asistencia/correcciones`, `/predios`, `/turnos`, `/eventos` y `/recursos`.
