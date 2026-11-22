<!--
Sync Impact Report
- Version change: plantilla sin ratificar → 1.0.0
- Principios definidos (antes eran marcadores de la plantilla):
  - [PRINCIPLE_1_NAME] → I. La especificación es la fuente de verdad
  - [PRINCIPLE_2_NAME] → II. Aislamiento estricto por junta (multi-tenant)
  - [PRINCIPLE_3_NAME] → III. Permisos por rol según la matriz de seguridad
  - [PRINCIPLE_4_NAME] → IV. Interfaz en español y responsive
  - [PRINCIPLE_5_NAME] → V. Simplicidad tecnológica
  - Nuevo → VI. Código sin comentarios
  - Nuevo → VII. Verificación contra criterios de aceptación
  - Nuevo → VIII. Trazabilidad requisito → caso de uso (CUxx)
- Secciones añadidas: Restricciones técnicas; Flujo de desarrollo y calidad; Gobernanza
- Secciones eliminadas: ninguna
- Plantillas dependientes: no se modifican (leen la constitución en tiempo de ejecución)
- TODO diferidos: ninguno
-->

# Constitución del Sistema Multi-Junta de Gestión Integral (AquaTenant)

## Principios fundamentales

### I. La especificación es la fuente de verdad

- Los documentos `docs/modulo-*.md`, `docs/modelo-dominio.md`, `docs/contexto/*.md` y los
  mockups de `docs/mockups/` (derivados del modelado UML del equipo, APE MDS 3) son la fuente
  de verdad del sistema.
- Todo código MUST derivarse de una especificación de Spec Kit (`spec.md` → `plan.md` →
  `tasks.md`). No se escribe código para comportamientos que no estén especificados.
- Si el código y la especificación discrepan, se corrige primero la especificación y luego se
  regenera o ajusta el código; nunca al revés.
- No se usan generadores externos (JHipster u otros) para producir código.

**Razón**: el proyecto se construye por desarrollo dirigido por especificaciones; cualquier
desviación silenciosa rompe la coherencia con el modelado UML.

### II. Aislamiento estricto por junta (multi-tenant)

- Toda entidad con datos de una junta MUST estar asociada a su junta.
- Toda consulta, alta, edición o eliminación de un usuario de junta MUST filtrarse por la junta
  del usuario autenticado; la junta se obtiene de la sesión, nunca de un parámetro enviado por
  el cliente.
- Acceder por URL o identificador a un recurso de otra junta MUST responder como no encontrado
  o prohibido, sin revelar datos.
- Solo SUPER_ADMIN ve todas las juntas, y únicamente para la gestión de plataforma
  (CU01, CU02, CU21, CU24); no entra a los módulos internos de las juntas.
- Cada funcionalidad MUST incluir al menos una verificación de que un usuario de la junta A no
  puede ver ni modificar datos de la junta B.

**Razón**: una sola instalación atiende a varias juntas; una fuga entre juntas es el fallo más
grave posible del sistema.

### III. Permisos por rol según la matriz de seguridad

- Los roles son exactamente SUPER_ADMIN, ADMIN_JUNTA, SECRETARIO, TESORERO y SOCIO.
- El acceso a cada caso de uso MUST coincidir con la matriz de `docs/contexto/seguridad.md`:
  "✔" es acceso completo, "ver" es solo lectura, y los permisos especiales (autoriza, solicita,
  recibe, ver públicas, solo con la directiva) se aplican tal como están escritos.
- Los permisos se aplican en el servidor (Spring Security y capa `service`); ocultar botones
  en la vista no basta.
- Una URL a la que el rol no tiene acceso MUST responder con una página 403 en español.
- Las contraseñas se almacenan con BCrypt.
- Cambiar la matriz requiere modificar primero `docs/contexto/seguridad.md`.

**Razón**: la directiva maneja dinero, multas y datos personales de los socios; cada rol debe
operar solo dentro de su responsabilidad.

### IV. Interfaz en español y responsive

- Todo texto visible (etiquetas, mensajes de validación, errores, páginas 403/404, reportes)
  MUST estar en español.
- Todas las vistas MUST ser usables en el navegador del computador y del celular
  (Bootstrap 5 responsive), porque reemplazan a la app móvil.
- Las vistas siguen los mockups de `docs/mockups/` y la paleta blanco/negro con verde
  #16A34A para acciones.

**Razón**: los usuarios son juntas del Ecuador y los socios acceden principalmente desde el
celular.

### V. Simplicidad tecnológica

- El stack es Java 17+, Spring Boot 3 (Web, Data JPA, Security, Validation), Thymeleaf,
  Bootstrap 5 por CDN, H2 en modo archivo y Maven. No se añade otra tecnología sin justificarla
  en el `plan.md` contra este principio.
- Prohibido: JHipster, frontend separado (React, Angular u otros), WebSocket, microservicios.
- Un solo módulo Maven, un solo servidor, una sola base de datos.
- Se elige la solución más simple que cumpla los criterios de aceptación (YAGNI).

**Razón**: el sistema debe poder ejecutarse con `mvn spring-boot:run` y mantenerse por un
equipo pequeño.

### VI. Código sin comentarios

- El código fuente (Java, plantillas Thymeleaf, SQL, configuración) MUST NOT llevar comentarios.
- La intención se expresa con nombres claros de clases, métodos y variables; la explicación
  vive en la especificación.

**Razón**: la especificación es la documentación; los comentarios se desactualizan y duplican
lo que ya dice el `spec.md`.

### VII. Verificación contra criterios de aceptación

- Cada caso de uso implementado MUST verificarse contra todos sus criterios de aceptación
  (flujo principal, flujos alternos y excepciones) antes de darse por terminado.
- La verificación incluye pruebas automatizadas donde sea posible y la comprobación en la
  aplicación ejecutándose con los datos de prueba.
- Un caso de uso con un criterio sin cumplir no está terminado.

**Razón**: los criterios de aceptación son el contrato con el equipo y con las juntas.

### VIII. Trazabilidad requisito → caso de uso (CUxx)

- Cada requisito funcional del `spec.md` MUST citar el caso de uso del que proviene (CU01 a
  CU32).
- Cada tarea de `tasks.md` y cada prueba MUST indicar el CUxx que cubre.
- Un requisito sin CUxx se rechaza o se registra primero como nuevo caso de uso en los
  documentos de `docs/`.

**Razón**: permite comprobar que todo caso de uso modelado está implementado y que no existe
funcionalidad sin origen.

## Restricciones técnicas

- Base de datos H2 en archivo (`jdbc:h2:file:./data/multijunta`); consola H2 solo en desarrollo.
- Paquete base `ec.uta.multijunta` con capas `model`, `repository`, `service`, `rest`, `config`;
  dependencias permitidas: `rest → service → repository → model`, `rest → model`,
  `service → model`. Las reglas de negocio viven en `service`.
- Fechas con `LocalDate`, horas con `LocalTime`, montos con `BigDecimal`.
- Reportes PDF con OpenPDF y Excel con Apache POI; recibos y "Mi información" como HTML
  imprimible.
- Archivos subidos en `./uploads`, máximo 5 MB.
- No se envían correos: las credenciales generadas se muestran en pantalla una sola vez.
- Datos de prueba cargados al arrancar: 2 juntas y un usuario por rol en cada una, documentados
  en el README.
- Fuera de alcance: app móvil, envío de correos, autocompletado por servicio externo de cédula,
  modo sin conexión, chat en tiempo real.

## Flujo de desarrollo y calidad

- Orden de trabajo: `/speckit-specify` → `/speckit-clarify` → `/speckit-plan` →
  `/speckit-tasks` → `/speckit-analyze` → `/speckit-implement`.
- El `plan.md` de cada funcionalidad MUST incluir una verificación de la constitución que revise
  los ocho principios; toda excepción se justifica por escrito en el plan.
- Puertas de calidad antes de cerrar una funcionalidad: compila sin errores, pruebas en verde,
  aislamiento entre juntas comprobado, permisos por rol comprobados, criterios de aceptación
  cumplidos, ningún comentario en el código y textos en español.

## Gobernanza

- Esta constitución prevalece sobre cualquier otra práctica del proyecto.
- Toda enmienda se hace con `/speckit-constitution`, se documenta en el Sync Impact Report y
  actualiza la versión y la fecha de última enmienda.
- Versionado semántico: MAJOR al eliminar o redefinir un principio; MINOR al añadir un principio
  o sección o ampliar su alcance; PATCH en aclaraciones y redacción.
- Cada revisión de `spec.md`, `plan.md`, `tasks.md` y del código MUST comprobar el cumplimiento
  de esta constitución; el incumplimiento bloquea la integración.

**Version**: 1.0.0 | **Ratified**: 2026-11-22 | **Last Amended**: 2026-11-22
