# Contrato: rutas de la interfaz (002)

Las rutas de 001 siguen vigentes. Todas las respuestas son HTML en español; los POST llevan
CSRF. Los permisos por rol salen de `specs/001-base-acceso-sesion/contracts/matriz-permisos.md`
(filas `/plataforma`, `/plataforma/juntas`, `/junta/configuracion`, `/usuarios`, `/panel`).

| Método y ruta | Rol | Comportamiento | CU / FR |
|---|---|---|---|
| GET `/plataforma` | SUPER_ADMIN | redirige a `/plataforma/juntas` | FR-001 |
| GET `/plataforma/juntas` | SUPER_ADMIN | listado de juntas con botón "+ Nueva Organización", íconos de lápiz, llave, "Suspender"/"Reactivar" | CU01, CU24 |
| POST `/plataforma/juntas` | SUPER_ADMIN | crea junta (`nombre`, `ruc`, `tipo`, `provincia`, `canton`); éxito → listado con "Junta registrada con éxito"; error → listado con el modal abierto y errores | CU01 |
| POST `/plataforma/juntas/{id}` | SUPER_ADMIN | edita junta; éxito → "Junta actualizada con éxito"; error → modal de edición abierto | CU24 |
| POST `/plataforma/juntas/{id}/suspender` | SUPER_ADMIN | `motivo` obligatorio; éxito → "Junta suspendida" | CU24 |
| POST `/plataforma/juntas/{id}/reactivar` | SUPER_ADMIN | reactiva; éxito → "Junta reactivada" | CU24 |
| POST `/plataforma/juntas/{id}/administradores` | SUPER_ADMIN | `cedula`, `nombres`, `apellidos`, `email`, opcional `reemplazadoId`; éxito → listado con resumen de credenciales (flash); error → panel lateral abierto | CU02 |
| GET `/panel` | ADMIN_JUNTA, SECRETARIO, TESORERO | tarjetas del rol | CU25 |
| GET `/junta/configuracion` | ADMIN_JUNTA | formulario con pestañas | CU03 |
| POST `/junta/configuracion` | ADMIN_JUNTA | multipart: `telefono`, `direccion`, `montoMultaInasistencia`, `logo`, `sectoresNuevos[]`, `sectoresQuitados[]`; éxito → "Datos de la junta actualizados"; error → formulario con `pestana` activa y foco | CU03 |
| GET `/logo/{juntaId}` | usuario de esa junta o SUPER_ADMIN | imagen del logo; otra junta o sin logo → 404 | CU03 FR-015 |
| GET `/usuarios` | ADMIN_JUNTA | redirige a `/usuarios/socios` | CU04 |
| GET `/usuarios/socios` | ADMIN_JUNTA | nómina con estado de cuenta y casillas | CU04 |
| POST `/usuarios/socios/credenciales` | ADMIN_JUNTA | `socioIds[]`; éxito → `/usuarios/credenciales` con la lista (flash); sin selección → "Seleccione al menos un socio" | CU04 |
| GET `/usuarios/credenciales` | ADMIN_JUNTA | lista imprimible si viene en flash; si no, redirige a `/usuarios/socios` | CU04 |
| POST `/usuarios/socios/{id}/desactivar` | ADMIN_JUNTA | desactiva; otra junta → 404 | CU04 |
| POST `/usuarios/socios/{id}/reactivar` | ADMIN_JUNTA | reactiva; otra junta → 404 | CU04 |
| GET `/usuarios/directiva` | ADMIN_JUNTA | cuentas de la directiva y formulario "Nueva cuenta" | CU23 |
| POST `/usuarios/directiva` | ADMIN_JUNTA | crea SECRETARIO/TESORERO; rol distinto → 403; éxito → contraseña temporal (flash) | CU23 |
| POST `/usuarios/directiva/{id}` | ADMIN_JUNTA | edita nombres, apellidos, correo, rol; cuenta ADMIN_JUNTA → 403; otra junta → 404 | CU23 |
| POST `/usuarios/directiva/{id}/desactivar` y `/reactivar` | ADMIN_JUNTA | cambia estado; cuenta ADMIN_JUNTA → 403; otra junta → 404 | CU23 |

## Cambios a rutas de 001

- `UsuarioActual.rutaInicio()` de SUPER_ADMIN pasa a `/plataforma/juntas`.
- `EnConstruccionController` deja de atender `/plataforma/juntas`, `/junta/configuracion` y
  `/usuarios`.
- La fila `/plataforma` de la matriz no muestra texto en el menú.
- `GET /logo/{juntaId}` queda fuera de la matriz: requiere sesión y el control de junta lo hace
  el controlador.
