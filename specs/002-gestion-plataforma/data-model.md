# Data Model: Módulo Gestión de Plataforma

Base: `docs/modelo-dominio.md` y `specs/001-base-acceso-sesion/data-model.md`. No se crean
tablas nuevas; se usan campos existentes con reglas nuevas o más estrictas. Las restricciones
de 001 siguen vigentes y no se repiten aquí salvo que esta funcionalidad las amplíe.

## Junta (existente)

| Campo | Regla en esta funcionalidad | Quién la edita |
|---|---|---|
| nombre | obligatorio, hasta 150 | SUPER_ADMIN (CU01, CU24) |
| ruc | exactamente 13 dígitos, único en la plataforma; mensaje "Este RUC ya está registrado" | SUPER_ADMIN |
| tipo | obligatorio, RIEGO / PARROQUIAL / COMUNITARIA | SUPER_ADMIN |
| provincia | obligatoria, debe existir en el catálogo | SUPER_ADMIN |
| canton | obligatorio, debe pertenecer a `provincia` en el catálogo | SUPER_ADMIN |
| fechaRegistro | se asigna al crear | sistema |
| direccion | obligatoria al guardar en CU03, hasta 200 | ADMIN_JUNTA (CU03) |
| telefono | opcional, hasta 20, solo dígitos, espacios, `+` y `-` | ADMIN_JUNTA |
| logoUrl | nombre del archivo en `./uploads/logos`; PNG o JPG hasta 5 MB | ADMIN_JUNTA |
| montoMultaInasistencia | opcional, ≥ 0, hasta 2 decimales | ADMIN_JUNTA |
| activa | `true` al crear; `false` al suspender | SUPER_ADMIN (CU24) |
| motivoSuspension | obligatorio al suspender, hasta 300; se borra al reactivar | SUPER_ADMIN |

Transiciones (CU24): `activa --Suspender(motivo)--> suspendida --Reactivar--> activa`. No hay
eliminación.

## Usuario (existente) — cuentas administrativas y de la directiva

| Campo | Regla en esta funcionalidad |
|---|---|
| cedula | 10 dígitos, única en la plataforma (001); mensaje "Esta cédula ya está registrada en la plataforma" |
| username | igual a la cédula en cuentas creadas por CU02, CU04 y CU23 |
| nombres, apellidos | obligatorios, hasta 100 |
| email | obligatorio en los formularios de CU02 y CU23 (no es restricción de la entidad: las cuentas existentes de 001 pueden no tener correo); único (sin distinguir mayúsculas) entre cuentas ADMIN_JUNTA, SECRETARIO y TESORERO; se guarda en minúsculas |
| rol | CU02 crea ADMIN_JUNTA; CU23 solo crea o cambia entre SECRETARIO y TESORERO |
| junta | la de la junta destino (CU02) o la del administrador autenticado (CU23) |
| activo | CU02 reasignar → `false` en la cuenta reemplazada; CU23 y CU04 desactivar/reactivar |
| debeCambiarPassword | `true` en toda cuenta creada o con contraseña nueva |

Regla de conteo (CU02): como máximo 2 usuarios con `rol = ADMIN_JUNTA` y `activo = true` por
junta.

## Socio (existente) — estado de cuenta derivado (CU04)

| `tieneCuenta` | `activo` | EstadoCuenta |
|---|---|---|
| false | (cualquiera) | SIN_CUENTA |
| true | true | ACTIVA |
| true | false | DESACTIVADA |

Transiciones: `SIN_CUENTA --Crear credenciales (solo estado ACTIVO en nómina)--> ACTIVA
--Desactivar--> DESACTIVADA --Reactivar--> ACTIVA`.

## Sector (existente)

| Campo | Regla en esta funcionalidad |
|---|---|
| nombre | obligatorio, hasta 80, único en la junta sin distinguir mayúsculas ni espacios extremos |
| junta | siempre la del administrador autenticado |

Regla: no se quita un sector con socios asignados.

## EstadoCuenta (nuevo enum, no persistido)

`SIN_CUENTA`, `ACTIVA`, `DESACTIVADA`, con etiquetas "Sin cuenta", "Activa", "Desactivada".

## Catálogo de ubicación (nuevo, no persistido)

`Provincia(nombre, List<String> cantones)` cargado desde `catalogos/ecuador-dpa.csv`: 24
provincias; cada cantón pertenece a una sola provincia.

## Credencial (nuevo, no persistido)

`Credencial(nombreCompleto, username, passwordTemporal)` para mostrar una sola vez.

## Tarjeta del panel (nuevo, no persistido, CU25)

`Tarjeta(titulo, icono, ruta, valor)`; `valor` vacío significa "Disponible próximamente".
