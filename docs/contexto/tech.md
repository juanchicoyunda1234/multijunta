# Stack tecnológico

- Java 17 o superior
- Spring Boot 3 (Spring Web, Spring Data JPA, Spring Security, Validation, Thymeleaf)
- Thymeleaf con Bootstrap 5 por CDN para las vistas (interfaz responsive, también usable desde el celular)
- Base de datos H2 en modo archivo (`jdbc:h2:file:./data/multijunta`), consola H2 habilitada en desarrollo
- Maven como gestor de dependencias
- Se ejecuta con `mvn spring-boot:run` y se abre en `http://localhost:8080`

## Reglas
- No usar JHipster ni generadores externos: el código se genera desde las especificaciones de Spec Kit.
- Sin frontend separado (no React ni Angular).
- Contraseñas con BCrypt.
- Permisos por rol según `docs/contexto/seguridad.md`.
- Datos de prueba cargados al arrancar (`data.sql` o un `CommandLineRunner`): 2 juntas, un usuario por rol en cada junta, socios, sectores, predios, eventos.
- Usuarios de prueba documentados en el README.
- El código no lleva comentarios.
- Textos de la interfaz en español.
- Fechas con `LocalDate`, horas con `LocalTime`, montos con `BigDecimal`.
- Reportes PDF con OpenPDF y Excel con Apache POI (CU12). Recibos y "Mi información" como página HTML imprimible.
- Archivos subidos (fotos, PDF de actas, facturas, oficios) se guardan en la carpeta `./uploads`, con límite de 5 MB.
- No se envían correos: las credenciales generadas se muestran en pantalla una sola vez.
- Las publicaciones programadas (CU15) se revisan con una tarea `@Scheduled`.
- El mapa de predios usa Leaflet por CDN con OpenStreetMap; se guardan latitud y longitud.
