# Contrato: rutas de la interfaz

Todas las respuestas son páginas HTML en español. Los formularios POST llevan token CSRF.
"Autenticado" incluye a cualquier rol.

| Método y ruta | Acceso | Comportamiento | CU / FR |
|---|---|---|---|
| GET `/login` | público | formulario de inicio; muestra el mensaje según el parámetro: `error`, `bloqueada`, `desactivada`, `suspendida`, `expirada`, `salio` | CU20 FR-001 |
| POST `/login` | público | campos `username`, `password`; éxito → redirección a `/inicio`; fallo → `/login?<motivo>` | CU20 FR-002, FR-005 a FR-007 |
| POST `/logout` | autenticado | cierra la sesión → `/login?salio` | CU20 FR-008 |
| GET `/` | autenticado | alias de `/inicio`; sin sesión lleva a `/login` | CU20 FR-003 |
| GET `/inicio` | autenticado | redirige según rol: SUPER_ADMIN → `/plataforma`, directiva → `/panel`, SOCIO → `/mi-informacion` | CU20 FR-003 |
| GET `/plataforma` | SUPER_ADMIN | inicio provisional (listado de juntas llega con CU01) | CU20 FR-003 |
| GET `/panel` | ADMIN_JUNTA, SECRETARIO, TESORERO | inicio provisional de la directiva (CU25 posterior) | CU20 FR-003 |
| GET `/mi-informacion` | SOCIO | inicio provisional del socio (CU17 posterior) | CU20 FR-003 |
| GET `/recuperar` | público | formulario de cédula o usuario | CU21 FR-022 |
| POST `/recuperar` | público | campo `identificador`; siempre → `/recuperar?enviada` con "Tu solicitud fue enviada a la directiva" | CU21 FR-023 |
| GET `/recuperacion/solicitudes` | ADMIN_JUNTA, SUPER_ADMIN | lista de solicitudes pendientes que el usuario puede atender | CU21 FR-024 |
| POST `/recuperacion/solicitudes/{id}/restablecer` | ADMIN_JUNTA, SUPER_ADMIN | restablece; redirige a la lista con la contraseña temporal en atributo flash; ya atendida → mensaje "La solicitud ya fue atendida"; ajena → 404 | CU21 FR-025, FR-026 |
| GET `/cuenta/cambiar-password` | autenticado | formulario actual, nueva, confirmación | CU22 FR-028 |
| POST `/cuenta/cambiar-password` | autenticado | campos `actual`, `nueva`, `confirmacion`; errores en el mismo formulario; éxito → redirección directa a la pantalla de inicio del rol (`/plataforma`, `/panel` o `/mi-informacion`) con "Contraseña actualizada correctamente" | CU22 FR-029 a FR-031 |
| GET `/notificaciones` | autenticado | lista propia, no leídas primero | CU29 FR-033 |
| GET `/notificaciones/{id}` | autenticado | marca como leída y redirige a su enlace; ajena → 404 | CU29 FR-034, FR-036 |
| POST `/notificaciones/marcar-todas` | autenticado | marca todas como leídas → `/notificaciones` | CU29 FR-035 |
| GET `/en-construccion` | autenticado | página "Módulo en construcción" | FR-018 |
| GET prefijos de la matriz | según `matriz-permisos.md` | mientras el módulo no exista, redirige a `/en-construccion` | FR-013, FR-018 |
| cualquier ruta sin permiso | — | página 403 "Acceso denegado" con enlace a `/inicio` | FR-014 |
| ruta inexistente | — | página 404 "Página no encontrada" | FR-020 |

## Filtros transversales

- Sesión de un usuario con `debeCambiarPassword = true`: toda ruta distinta de
  `/cuenta/cambiar-password`, `/logout` y recursos estáticos → redirección a
  `/cuenta/cambiar-password` (FR-004).
- Cuenta desactivada o junta suspendida durante la sesión: sesión invalidada →
  `/login?desactivada` o `/login?suspendida` (FR-010).
- Sesión expirada (30 min) → `/login?expirada` (FR-009).

## Mensajes exactos

| Clave | Texto |
|---|---|
| login.error | Usuario o contraseña inválidos |
| login.bloqueada | Cuenta bloqueada temporalmente, intente de nuevo en 15 minutos |
| login.desactivada | Cuenta desactivada, contacte a la directiva |
| login.suspendida | La organización se encuentra suspendida |
| login.expirada | Tu sesión expiró, inicia sesión de nuevo |
| recuperar.enviada | Tu solicitud fue enviada a la directiva |
| recuperar.atendida | La solicitud ya fue atendida |
| password.actualIncorrecta | La contraseña actual es incorrecta |
| password.noCoincide | La confirmación no coincide con la nueva contraseña |
| password.igualAnterior | La nueva contraseña debe ser distinta de la anterior |
| password.formato | La contraseña debe tener al menos 8 caracteres, con al menos una letra y un número |
| password.actualizada | Contraseña actualizada correctamente |
| notificacion.recuperacion.titulo | Nueva solicitud de recuperación de contraseña |
