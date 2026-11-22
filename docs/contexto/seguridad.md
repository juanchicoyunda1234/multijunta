# Permisos por rol

Cada usuario solo opera sobre los datos de su junta. El Super Administrador no entra a los módulos internos de las juntas.

| Caso de uso | SUPER_ADMIN | ADMIN_JUNTA | SECRETARIO | TESORERO | SOCIO |
|---|---|---|---|---|---|
| CU01 Registrar nuevas juntas | ✔ | | | | |
| CU02 Asignar Administrador de la Junta | ✔ | | | | |
| CU24 Editar o suspender juntas | ✔ | | | | |
| CU03 Configurar datos generales y sectores | | ✔ | | | |
| CU04 Generar accesos de socios | | ✔ | | | |
| CU23 Cuentas de la directiva | | ✔ | | | |
| CU21 Atender recuperación de contraseña | ✔ (solo administradores de junta) | ✔ | | | |
| CU25 Panel de inicio de la directiva | | ✔ | ✔ | ✔ | |
| CU05 Nómina de socios | | ver | ✔ | ver | |
| CU06 Registrar asistencia | | ver | ✔ | ver | |
| CU07 Predios y mapa | | ver | ✔ | | |
| CU08 Calendario y turnos | | ver | ✔ | | |
| CU26 Eventos | | ver | ✔ | | |
| CU27 Recursos de riego | | ver | ✔ | | |
| CU28 Corregir asistencia cerrada | | autoriza | solicita | | |
| CU09 Registrar pagos | | ver | | ✔ | |
| CU10 Generar multas | | ver | ver | ✔ | |
| CU11 Egresos | | ver | | ✔ | |
| CU12 Reportes | | ✔ | | ✔ | |
| CU13 Inventario | | ver | | ✔ | |
| CU14 Activos fijos | | ver | | ✔ | |
| CU30 Cuotas y obligaciones | | ver | | ✔ | |
| CU31 Reversar pago | | | | ✔ | |
| CU15 Emitir convocatorias | | ver | ✔ | | recibe |
| CU16 Actas | | ver | ✔ | ver | ver públicas |
| CU19 Comunicaciones externas | | ver | ✔ | | |
| CU17 Mi información | | | | | ✔ |
| CU32 Anuncios y repositorio | | | | | ✔ |
| CU18 Mensajes | | ✔ | ✔ | ✔ | ✔ (solo con la directiva) |
| CU20 Sesión, CU22 Cambiar contraseña, CU29 Notificaciones | ✔ | ✔ | ✔ | ✔ | ✔ |

"ver" significa acceso de solo lectura. Una URL a la que el rol no tiene acceso responde con una página 403 en español.
