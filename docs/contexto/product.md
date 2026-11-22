# Producto: Sistema Multi-Junta de Gestión Integral

## Qué es
Aplicación web para juntas de riego, juntas parroquiales y juntas comunitarias del Ecuador. Reemplaza el control manual (cuadernos, hojas sueltas) de socios, predios, turnos de riego, asistencia a mingas y asambleas, multas, pagos, egresos, bienes y comunicaciones.

Nombre comercial usado en los mockups: **AquaTenant**. Paleta de los mockups: blanco y negro con verde #16A34A para acciones.

## Multi-junta (multi-tenant)
- Una sola instalación atiende a varias juntas.
- Cada usuario pertenece a una junta y solo ve los datos de su junta.
- El Super Administrador de la Plataforma es el único que ve todas las juntas.

## Actores y roles
| Rol | Qué hace |
|---|---|
| SUPER_ADMIN (Super Administrador de la Plataforma) | Registra juntas y asigna su administrador |
| ADMIN_JUNTA (Administrador de la Junta) | Configura los datos de su junta, administra cuentas, envía mensajes |
| SECRETARIO (Secretario/a) | Nómina de socios, asistencia, predios, calendario y turnos, multas, convocatorias, actas, comunicaciones externas |
| TESORERO (Tesorero/a) | Pagos, multas, egresos, reportes financieros, inventario, activos fijos |
| SOCIO (Miembro / Socio) | Consulta su información, recibe convocatorias, envía y recibe mensajes |

## Módulos
1. Gestión de Plataforma: CU01 a CU04, más CU20 a CU25 (sesión, recuperación y cambio de contraseña, cuentas de la directiva, juntas, panel de inicio)
2. Gestión Administrativa: CU05 a CU08, más CU26 a CU28 (eventos, recursos de riego, corrección de asistencia)
3. Gestión Financiera: CU09 a CU14 (reutiliza CU06), más CU30 y CU31 (cuotas, reverso de pagos)
4. Gestión de Comunicaciones: CU15 a CU19, más CU29 y CU32 (notificaciones, anuncios y repositorio del socio)

Los CU20 a CU32 se identificaron al escribir esta especificación: el modelado UML los suponía pero no los tenía.

El detalle de cada caso de uso está en `docs/modulo-*.md` y el modelo de datos en `docs/modelo-dominio.md`. Esos documentos vienen del modelado UML y de las especificaciones de casos de uso del equipo (APE MDS 3) y son la fuente de verdad. Los mockups están en `docs/mockups/`.

## Fuera de alcance
- App móvil (.apk): se reemplaza por interfaz web responsive.
- Envío de correos.
- Autocompletado de nombres por servicio externo de cédula.
- Modo sin conexión.
- Chat en tiempo real con WebSocket.
