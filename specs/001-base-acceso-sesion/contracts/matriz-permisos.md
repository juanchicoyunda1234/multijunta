# Contrato: matriz de permisos y menú

Traducción de `docs/contexto/seguridad.md` a rutas. C = acceso completo (todos los métodos),
L = solo lectura (solo GET), — = 403. `MatrizPermisos` MUST contener exactamente estas filas;
el menú lateral muestra una fila si el rol tiene C o L.

| CU | Menú | Prefijo de ruta | SUPER_ADMIN | ADMIN_JUNTA | SECRETARIO | TESORERO | SOCIO |
|---|---|---|---|---|---|---|---|
| CU20 | Inicio | `/plataforma` (exacto, inicio provisional) | C | — | — | — | — |
| CU01, CU02, CU24 | Juntas | `/plataforma/juntas` | C | — | — | — | — |
| CU03 | Configuración de Junta | `/junta/configuracion` | — | C | — | — | — |
| CU04, CU23 | Usuarios | `/usuarios` | — | C | — | — | — |
| CU21 | Solicitudes de recuperación | `/recuperacion/solicitudes` | C | C | — | — | — |
| CU25 | Inicio | `/panel` | — | C | C | C | — |
| CU05 | Nómina de socios | `/socios` | — | L | C | L | — |
| CU06 | Asistencia | `/asistencia` | — | L | C | L | — |
| CU28 | Correcciones de asistencia | `/asistencia/correcciones` | — | C | C | — | — |
| CU07 | Predios | `/predios` | — | L | C | — | — |
| CU08 | Calendario y turnos | `/turnos` | — | L | C | — | — |
| CU26 | Eventos | `/eventos` | — | L | C | — | — |
| CU27 | Recursos de riego | `/recursos` | — | L | C | — | — |
| CU09 | Pagos | `/pagos` | — | L | — | C | — |
| CU10 | Multas | `/multas` | — | L | L | C | — |
| CU11 | Egresos | `/egresos` | — | L | — | C | — |
| CU12 | Reportes | `/reportes` | — | C | — | C | — |
| CU13 | Inventario | `/inventario` | — | L | — | C | — |
| CU14 | Activos fijos | `/activos` | — | L | — | C | — |
| CU30, CU31 | Cuotas | `/cuotas` | — | L | — | C | — |
| CU15 | Convocatorias | `/convocatorias` | — | L | C | — | — |
| CU16 | Actas | `/actas` | — | L | C | L | — |
| CU19 | Comunicaciones externas | `/comunicaciones` | — | L | C | — | — |
| CU17 | Mi información | `/mi-informacion` | — | — | — | — | C |
| CU32 | Anuncios y repositorio | `/anuncios` | — | — | — | — | C |
| CU18 | Mensajes | `/mensajes` | — | C | C | C | C |
| CU20, CU22, CU29 | (barra superior) | `/cuenta`, `/notificaciones`, `/inicio`, `/en-construccion` | C | C | C | C | C |

Rutas fuera de la matriz que MUST quedar accesibles: `/login`, `/logout`, `/recuperar` y
`/css/**` (públicas); `/error` (pública, para que 403, 404 y 500 se rendericen siempre);
`/h2-console/**` solo con el perfil `dev`. Cualquier otra ruta no listada requiere sesión
iniciada y, si no existe, responde 404 (HU2-6).
La fila `/plataforma` es exacta: no abre `/plataforma/juntas`, que tiene su propia fila.

## Permisos especiales (se refinan en el servicio de cada CU)

- CU28: SECRETARIO solicita la corrección y ADMIN_JUNTA la autoriza; ambos tienen C en
  `/asistencia/correcciones` y el servicio de CU28 decide qué operación corresponde a cada
  uno. La regla de `/asistencia/correcciones` se registra antes que la de `/asistencia` para
  que el prefijo más específico gane.
- CU31: solo TESORERO reversa pagos (C en `/cuotas` y `/pagos`); ADMIN_JUNTA solo lectura.
- CU15: SOCIO recibe convocatorias por notificación y en `/anuncios`, no en `/convocatorias`.
- CU16: SOCIO ve actas públicas en `/anuncios` (repositorio).
- CU18: SOCIO solo conversa con la directiva de su junta (regla del servicio de CU18).
- CU21: SUPER_ADMIN solo ve solicitudes de ADMIN_JUNTA; ADMIN_JUNTA solo las de su junta y
  de roles distintos de ADMIN_JUNTA.

## Verificación

`PermisosPorRolTest` recorre cada fila × rol, envía todas las peticiones con token CSRF
válido y comprueba: C → GET y POST no devuelven 403;
L → GET no devuelve 403 y POST devuelve 403; — → GET devuelve 403 (SC-003).
