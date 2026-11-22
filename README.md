# Sistema Multi-Junta de Gestión Integral (AquaTenant)

Aplicación web para juntas de riego, parroquiales y comunitarias del Ecuador. Una sola
instalación atiende a varias juntas; cada usuario solo ve los datos de su junta.

Especificaciones implementadas:

- `specs/001-base-acceso-sesion/`: inicio y cierre de sesión, recuperación y cambio de
  contraseña, permisos por rol, campana de notificaciones.
- `specs/002-gestion-plataforma/`: listado, registro, edición y suspensión de juntas (Super
  Administrador); asignación de administradores; configuración de la junta con sectores y logo;
  módulo Usuarios (accesos de socios y cuentas de la directiva); panel de la directiva.
- `specs/003-gestion-administrativa/`: nómina de socios, eventos, toma y cierre de asistencia
  (con la interfaz `IAsistencia` para el módulo financiero), recursos de riego, calendario
  semanal de turnos, predios con mapa (Leaflet + OpenStreetMap) y correcciones de asistencia con
  aprobación del administrador.

Las cuentas creadas desde la aplicación (administradores, directiva y socios) usan la **cédula
como usuario** y una contraseña temporal que se muestra una sola vez y debe cambiarse en el
primer ingreso. Los logos y fotos de predios se guardan en `./uploads`. El mapa de predios
necesita conexión a internet.

Los datos de prueba incluyen, por cada junta activa, dos recursos, una Asamblea de ayer lista
para tomar asistencia, una Minga y una Reunión próximas, dos turnos en la semana actual y un
predio por socio activo.

## Requisitos

- JDK 17 o superior (`java -version`).
- No hace falta tener Maven instalado: el proyecto incluye Maven Wrapper.

## Ejecutar

```powershell
.\mvnw.cmd spring-boot:run
```

Abrir `http://localhost:8080`. La base de datos H2 se guarda en `./data/multijunta`. La
primera vez que arranca con la base vacía se cargan los datos de prueba.

Para usar la consola H2 (`http://localhost:8080/h2-console`, URL JDBC
`jdbc:h2:file:./data/multijunta`, usuario `sa`, sin contraseña):

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

## Pruebas

```powershell
.\mvnw.cmd test
```

## Usuarios de prueba

Contraseña de todos los usuarios: `Multijunta2026`

| Usuario | Rol | Junta | Observación |
|---|---|---|---|
| `superadmin` | Super Administrador | (plataforma) | |
| `admin.sanpedro` | Administrador de la Junta | Junta de Riego San Pedro | |
| `secretaria.sanpedro` | Secretario/a | Junta de Riego San Pedro | |
| `tesorero.sanpedro` | Tesorero/a | Junta de Riego San Pedro | |
| `secretario.anterior` | Secretario/a | Junta de Riego San Pedro | Cuenta desactivada |
| `1800000101` | Socio | Junta de Riego San Pedro | Socio con cuenta |
| `admin.rosario` | Administrador de la Junta | Junta Comunitaria El Rosario | |
| `secretario.rosario` | Secretario/a | Junta Comunitaria El Rosario | |
| `tesorera.rosario` | Tesorero/a | Junta Comunitaria El Rosario | |
| `1800000201` | Socio | Junta Comunitaria El Rosario | Debe cambiar la contraseña al entrar |
| `admin.santarosa` | Administrador de la Junta | Junta Parroquial Santa Rosa | Junta suspendida |

Socios sin cuenta (no inician sesión): `1800000102` y `1800000202`. Socios inactivos:
`1800000103` y `1800000203`.
