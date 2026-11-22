# Research: Módulo Gestión de Plataforma

Sin puntos NEEDS CLARIFICATION. Decisiones de diseño:

## R1. Catálogo de provincias y cantones

- **Decision**: archivo `src/main/resources/catalogos/ecuador-dpa.csv` con columnas
  `provincia;canton` (24 provincias y sus 221 cantones según la División Político
  Administrativa del INEC), cargado una vez al arrancar por `CatalogoUbicacionService` en un
  mapa ordenado `provincia → cantones`. La vista recibe el catálogo como JSON en línea para
  filtrar cantones; el servidor valida el par provincia–cantón.
- **Rationale**: lista fija y pequeña; no requiere tabla ni pantalla de mantenimiento
  (Principio V). La validación en el servidor cubre FR-004 aunque el JavaScript falle.
- **Alternatives considered**: tablas `Provincia`/`Canton` en la base (más código sin
  beneficio); texto libre (rompe FR-004).

## R2. Ventana emergente (CU01, CU24) y panel lateral (CU02) sin frontend separado

- **Decision**: modal y offcanvas de Bootstrap dentro de `plataforma/juntas.html`. Los
  formularios hacen POST normal. Si hay errores, el controlador vuelve a renderizar el listado
  con el formulario, sus errores y un atributo `abrir` (`registro`, `edicion`, `administrador`,
  `suspension`) que un script en línea usa para reabrir el componente. Éxito → redirección con
  mensaje flash (patrón POST-redirect-GET de 001).
- **Rationale**: cumple "la ventana sigue abierta con lo escrito" sin AJAX ni librerías.
- **Alternatives considered**: páginas separadas por formulario (se aleja del mockup); fetch
  con JSON (introduce una API que la constitución no pide).

## R3. Pestañas y sectores de CU03

- **Decision**: un solo `<form multipart>` con `nav-tabs` de Bootstrap; las tres pestañas son
  paneles del mismo formulario, así lo escrito se conserva al cambiar de pestaña. Los sectores
  se editan en el cliente: "+ Añadir" agrega un ítem con un `<input type="hidden"
  name="sectoresNuevos">` y [X] agrega el id a `sectoresQuitados`. "Actualizar Datos" envía
  todo y `ConfiguracionJuntaService.actualizar` aplica cambios, altas y bajas en una
  transacción. Ante error, se renderiza de nuevo con `pestana` activa y `autofocus` en el
  campo. Duplicados y vacíos también se comprueban en el cliente para dar el mensaje al
  instante, y en el servidor como regla.
- **Rationale**: respeta el mockup (añadir y luego "Actualizar Datos") y el alterno de CU03.

## R4. Logo

- **Decision**: `spring.servlet.multipart.max-file-size=5MB` y `max-request-size=6MB`.
  `ArchivoService` valida tipo por contenido declarado y extensión (`image/png`, `image/jpeg`),
  guarda en `./uploads/logos/junta-{id}-{uuid}.{ext}`, borra el logo anterior y deja el nombre
  en `Junta.logoUrl`. `LogoController` sirve `GET /logo/{juntaId}` solo a usuarios de esa
  junta o al SUPER_ADMIN (404 en otro caso). `MaxUploadSizeExceededException` se traduce al
  mensaje de FR-015 en `VistaComunAdvice`.
- **Rationale**: sigue `tech.md` (carpeta `./uploads`, 5 MB) y el aislamiento por junta.

## R5. Unicidad de correo y límite de administradores

- **Decision**: reglas en servicio: `UsuarioRepository.existsByEmailIgnoreCaseAndRolIn(email,
  ADMIN_JUNTA, SECRETARIO, TESORERO)` (y la variante `AndIdNot` al editar) y
  `countByJuntaIdAndRolAndActivoTrue(juntaId, ADMIN_JUNTA)`. La cédula sigue protegida por la
  restricción única de 001 y se verifica antes de guardar para dar el mensaje de FR-011. Los
  correos se guardan en minúsculas.
- **Rationale**: H2 no ofrece índices únicos parciales por rol; la regla es de negocio.

## R6. Reasignación atómica (CU02)

- **Decision**: `AdministradorService.reasignar(juntaId, reemplazadoId, formulario)` en una
  transacción: valida el formulario y los duplicados primero, luego desactiva la cuenta
  reemplazada (debe ser ADMIN_JUNTA activo de esa junta, si no 404) y crea la nueva. Si algo
  falla, no cambia nada. El cierre de sesión del reemplazado lo hace `EstadoCuentaFilter`.

## R7. Credenciales mostradas una vez

- **Decision**: los servicios devuelven `List<Credencial>`; el controlador los pone como
  atributo flash y redirige a `/usuarios/credenciales` (CU04) o al listado (CU02, CU23), que
  los muestra con botón "Imprimir" (`window.print()` y estilos `@media print`). Recargar ya no
  los muestra. Contraseñas con `PasswordService.generarTemporal()` de 001.

## R8. Estado de cuenta del socio

- **Decision**: derivado, sin columna nueva: `!tieneCuenta → SIN_CUENTA`; `tieneCuenta &&
  activo → ACTIVA`; `tieneCuenta && !activo → DESACTIVADA` (`EstadoCuenta`). Crear cuenta:
  `tieneCuenta=true, activo=true, debeCambiarPassword=true`, nueva contraseña. Desactivar:
  `activo=false`. Reactivar: `activo=true` conservando la contraseña.

## R9. Panel de la directiva (CU25)

- **Decision**: `PanelService.tarjetas()` arma una lista fija de 8 definiciones (título,
  ícono, ruta del módulo, proveedor de valor opcional). Se filtran con
  `MatrizPermisos` por la ruta: visible si el rol tiene C o L sobre ella. Valores reales:
  socios activos (`SocioRepository.countByJuntaIdAndEstado`) y solicitudes pendientes
  (`RecuperacionService.listarAtendibles().size()`); las demás devuelven vacío →
  "Disponible próximamente". Rutas: socios `/socios`, eventos y turnos `/turnos`, asistencias
  `/asistencia`, saldo `/reportes`, deuda `/cuotas`, stock bajo `/inventario`, mensajes
  `/mensajes`, solicitudes `/recuperacion/solicitudes`. Con la matriz actual el resultado es
  exactamente el reparto por rol de la Historia 7.
- **Rationale**: la visibilidad no se duplica; nace de la misma matriz que el menú (001 R4).

## R10. Inicio del SUPER_ADMIN

- **Decision**: `UsuarioActual.rutaInicio()` devuelve `/plataforma/juntas` para SUPER_ADMIN;
  `GET /plataforma` redirige allí; la fila `/plataforma` de `MatrizPermisos` queda sin texto
  de menú para no duplicar "Juntas". Se ajustan las pruebas de 001 que esperaban `/plataforma`.

## R11. Cambio de rol en CU23 durante la sesión

- **Decision**: no se toca la sesión de la persona; `UsuarioAutenticado` guarda el rol al
  ingresar. Para que el rol nuevo rija desde la siguiente acción (FR-026), `EstadoCuentaFilter`
  compara el rol guardado con el de la base y, si cambió, reemplaza la autenticación de la
  sesión por una nueva con el rol actual.
- **Alternatives considered**: forzar cierre de sesión (más molesto para el usuario).

## R12. Pruebas

- **Decision**: una clase MockMvc por CU con datos propios (`Fabrica` de 001 ampliada con
  helpers de junta y socio sin cuenta). Cada clase de CU de junta incluye un caso A/B.
  `CatalogoUbicacionTest` valida 24 provincias, cantón por provincia y rechazo de pares
  inválidos.
