# Quickstart: validar el Módulo Gestión de Comunicaciones

## Preparación

1. Borrar `./data` y `./uploads` para partir de los datos de prueba (research R11).
2. Ejecutar las pruebas y arrancar la aplicación:

```powershell
.\mvnw.cmd test
```

```powershell
.\mvnw.cmd spring-boot:run
```

3. Abrir `http://localhost:8080`. Contraseña de todos: `Multijunta2026` (README). El socio con
   cuenta de San Pedro es `1800000101`.

## Escenarios (junta San Pedro)

| # | Usuario | Pasos | Resultado esperado |
|---|---|---|---|
| 1 | `1800000101` | Iniciar sesión | Llega a "Mi Panel"; en Resumen ve su ficha, deuda total, próximo turno y la Asamblea publicada fijada arriba (CU17, CU15) |
| 2 | socio | Estado de Cuenta → Imprimir estado de cuenta | Deuda total en rojo con su detalle y el pago `TX-000001`; la vista de impresión oculta el menú (CU17) |
| 3 | socio | Mi Predio y Calendario | Su predio en el mapa con foto si la tiene; turnos de su sector de las próximas 4 semanas; sin botones de guardar (CU17) |
| 4 | socio | Anuncios y Repositorio | La Asamblea fijada arriba; la Minga en borrador no aparece; solo el acta pública, que se abre y descarga (CU32) |
| 5 | socio | Mensajes → "Directiva" → escribir y enviar; adjuntar una imagen PNG; intentar enviar vacío | Burbujas propias en su color; miniatura de la imagen; botón Enviar deshabilitado sin texto (CU18) |
| 6 | `admin.sanpedro` | Ver el contador de "Mensajes" y la campana sin recargar (esperar 15 s); abrir la conversación y responder | Contadores aumentan solos; al abrir bajan; el socio ve la respuesta como "Directiva" (CU18, CU29) |
| 7 | `secretaria.sanpedro` | Convocatorias → Redactar → Urgente, Asamblea para dentro de 10 días → Publicar Inmediatamente | "Convocatoria publicada"; el socio recibe "Nueva convocatoria" en la campana (CU15, CU29) |
| 8 | secretaria | Redactar una Minga Normal con publicación dentro de 2 minutos; esperar | Queda en borrador y se publica sola en menos de 1 minuto después de su hora (CU15) |
| 9 | secretaria | Redactar con fecha de evento pasada | "La fecha debe ser futura" (CU15) |
| 10 | secretaria | Eventos → crear una Asamblea → "Crear convocatoria" | El formulario llega con tipo, fecha, hora y lugar precargados (CU26, CU15) |
| 11 | secretaria | Actas → Subir Documento con PDF público y otro sin "Público en Portal Socios"; intentar un PNG | El socio ve solo el público; "Formato no válido. Solo se admiten archivos PDF por seguridad." (CU16) |
| 12 | secretaria | Comunicaciones externas → registrar con PDF y otra sin documento; adjuntar después; eliminar la última y registrar otra | Números `CE-AAAA-0003`, `0004`, …; el eliminado no se reutiliza; "Pendiente de digitalización" desaparece al adjuntar (CU19) |
| 13 | `tesorero.sanpedro` | Pagos → Cuotas → emitir una cuota a un socio con cuenta | El socio recibe "Nueva cuota" con enlace a su Estado de Cuenta (CU29) |
| 14 | `admin.sanpedro` y `tesorero.sanpedro` | Abrir Convocatorias, Actas y Comunicaciones; intentar un POST; abrir `/mi-informacion` | Lectura según la matriz y 403 al modificar; `/mi-informacion` responde 403; la tarjeta "Mensajes no leídos" muestra el número real (FR-029, FR-031) |
| 15 | todos | Recorrer las pantallas nuevas a 360 px | Sin desplazamiento horizontal (FR-032) |

Al terminar, borrar `./data` y `./uploads` para dejar los datos de prueba limpios.
