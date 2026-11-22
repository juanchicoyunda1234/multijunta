# Quickstart: validar la base del sistema

## Requisitos

- JDK 17 o superior (`java -version`).
- No hace falta Maven instalado: se usa el wrapper incluido.

## Ejecutar

```powershell
.\mvnw.cmd spring-boot:run
```

Abrir `http://localhost:8080`. La primera vez se crean los datos de prueba (ver README:
usuarios, contraseña común, rol y junta). La consola H2 está en `/h2-console` solo con el
perfil `dev`.

## Pruebas automáticas

```powershell
.\mvnw.cmd test
```

Resultado esperado: todas las clases en verde (`LoginTest`, `CambioPasswordTest`,
`RecuperacionTest`, `NotificacionTest`, `PermisosPorRolTest`, `AislamientoJuntaTest`,
`DatosPruebaTest`).

## Validación manual (criterios de aceptación)

Usar los usuarios del README. Mensajes exactos en [contracts/rutas-ui.md](contracts/rutas-ui.md).

| # | Pasos | Resultado esperado | Spec |
|---|---|---|---|
| 1 | Abrir `/socios` sin sesión | Formulario de inicio de sesión | HU1-1 |
| 2 | Entrar con el SUPER_ADMIN, con un ADMIN_JUNTA, un SECRETARIO, un TESORERO y un SOCIO con cuenta | Llegan a `/plataforma`, `/panel` o `/mi-informacion` según el rol; barra con nombre, rol y junta | HU1-2..4, HU2-1 |
| 3 | Entrar con el usuario marcado "debe cambiar contraseña" y abrir otra página | Vuelve a "Cambiar contraseña" | HU1-5 |
| 4 | Contraseña incorrecta | "Usuario o contraseña inválidos" | HU1-6 |
| 5 | Entrar con el usuario desactivado y con el de la junta suspendida | Mensajes de cuenta desactivada y organización suspendida | HU1-7, HU1-8 |
| 6 | Cinco contraseñas incorrectas seguidas y luego la correcta | Mensaje de bloqueo de 15 minutos | HU1-9 |
| 7 | Cerrar sesión y usar "atrás" | Vuelve al inicio de sesión, sin datos visibles | HU1-11 |
| 8 | Con cada rol, revisar el menú contra [matriz-permisos.md](contracts/matriz-permisos.md) | Solo aparecen las opciones permitidas | HU2-1 |
| 9 | Como SOCIO abrir `/pagos`; como SUPER_ADMIN abrir `/socios` | Página 403 en español | HU2-2, HU2-5 |
| 10 | Como ADMIN_JUNTA de la Junta 1, abrir `/recuperacion/solicitudes/{id}/restablecer` de una solicitud de la Junta 2 (por POST) | 404, sin datos | HU2-4 |
| 11 | En `/recuperar` enviar la cédula de un socio; luego una cédula inexistente | Mismo mensaje en ambos casos; solo el primero crea solicitud | HU4-1, HU4-5 |
| 12 | Entrar como ADMIN_JUNTA de esa junta | Campana con 1; abrirla lleva a solicitudes | HU5-1..5 |
| 13 | Restablecer la solicitud; recargar | Contraseña temporal visible una vez; tras recargar ya no | HU4-3, HU4-8 |
| 14 | Entrar como el socio con la temporal; probar las tres excepciones de CU22 y un cambio válido | Mensajes exactos; luego llega a su inicio | HU3-2..7 |
| 15 | Abrir la app en el celular (o ventana de 360 px) | Sin desplazamiento horizontal; menú colapsable | SC-006 |
| 16 | Reiniciar la aplicación | Los datos de prueba no se duplican | HU6-2 |
