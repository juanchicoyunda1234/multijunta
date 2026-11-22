# Quickstart: validar el módulo Gestión de Plataforma

## Requisitos y ejecución

Igual que `specs/001-base-acceso-sesion/quickstart.md`:

```powershell
.\mvnw.cmd spring-boot:run
```

```powershell
.\mvnw.cmd test
```

Resultado esperado de las pruebas: todas en verde, incluidas `JuntaTest`, `AdministradorTest`,
`ConfiguracionJuntaTest`, `CuentasSociosTest`, `DirectivaTest`, `PanelTest` y
`CatalogoUbicacionTest`, además de las de 001.

Para empezar desde los datos de prueba originales, detener la aplicación y borrar la carpeta
`data/` antes de arrancar. Usuarios y contraseña común en el README.

## Validación manual

Rutas en [contracts/rutas-ui.md](contracts/rutas-ui.md); mensajes en
[contracts/mensajes.md](contracts/mensajes.md).

| # | Pasos | Resultado esperado | Spec |
|---|---|---|---|
| 1 | Entrar como `superadmin` | Llega al listado de juntas con las 3 juntas de prueba | HU1-1 |
| 2 | "+ Nueva Organización", elegir provincia y luego cantón, guardar | El cantón solo lista los de la provincia; "Junta registrada con éxito" | HU1-2, HU1-3 |
| 3 | Registrar otra con el RUC `1890000001001` | El modal sigue abierto, RUC en rojo, "Este RUC ya está registrado" | HU1-5 |
| 4 | Llave de la junta nueva: asignar administrador | Resumen con usuario (cédula) y contraseña, una vez | HU2-1, HU2-2 |
| 5 | Asignar un segundo y luego un tercero sin reasignar | El tercero se rechaza con el mensaje de límite | HU2-3 |
| 6 | Reasignar reemplazando al primero | El primero queda inactivo y no entra | HU2-4 |
| 7 | Cerrar sesión y entrar con el administrador nuevo | Pide cambiar la contraseña | HU2-7 |
| 8 | Como `admin.sanpedro`, "Configuración de Junta": vaciar la dirección y guardar | Se abre la pestaña Ubicación con el foco y el mensaje | HU3-4 |
| 9 | Completar dirección, subir un logo PNG, añadir "Sector Este", intentar quitar "Sector Norte", guardar | Logo en la barra; "Sector Este" guardado; "Sector Norte" no se quita (tiene socios) | HU3-3, HU3-5, HU3-6 |
| 10 | Usuarios → Socios: seleccionar los sin cuenta y crear credenciales | Lista imprimible; el socio INACTIVO no es seleccionable | HU4-2, HU4-4 |
| 11 | Desactivar y reactivar una cuenta de socio | El badge cambia; desactivado no entra | HU4-6, HU4-7 |
| 12 | Usuarios → Directiva: crear un secretario, cambiarlo a tesorero, desactivarlo | Mensajes de éxito; el menú de esa persona cambia en su siguiente acción | HU5-2..4 |
| 13 | Como `superadmin`, suspender "Junta Comunitaria El Rosario" con motivo; entrar con `admin.rosario`; reactivar | No entra mientras está suspendida; entra tras reactivar | HU6-3..5 |
| 14 | Entrar como `admin.sanpedro`, `secretaria.sanpedro` y `tesorero.sanpedro` | Cada panel muestra solo sus tarjetas; "Socios activos" = 2 en San Pedro | HU7-1..3 |
| 15 | Abrir cada pantalla a 360 px | Sin desplazamiento horizontal de la página | SC-007 |
