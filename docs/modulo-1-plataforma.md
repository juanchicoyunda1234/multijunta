# Módulo 1: Gestión de Plataforma

Mockups: `docs/mockups/CU01` a `CU04`.

## CU01 Registrar nuevas juntas
- Actor: Super Administrador de la Plataforma. Prioridad alta.
- Disparador: botón "+ Nueva Organización" en el listado de juntas.
- Precondición: sesión iniciada como Super Administrador.
- Flujo: el sistema abre un modal "Registro de Nueva Junta" con Nombre, RUC, Provincia y Cantón (selectores) → ingresa los datos → clic en "Guardar Organización" → el sistema guarda, cierra el modal, actualiza la tabla y muestra la alerta verde "Junta registrada con éxito".
- Alterno: "Cancelar" cierra el modal sin guardar.
- Excepción: si el RUC ya existe, el modal sigue abierto, el campo se marca en rojo y aparece "Este RUC ya está registrado".
- Reglas: no hay dos juntas con el mismo RUC; el identificador se genera automáticamente; RUC de 13 dígitos.

## CU02 Asignar rol de Administrador de la Junta
- Actor: Super Administrador de la Plataforma. Prioridad alta.
- Disparador: ícono de llave "Asignar Administrador" en la fila de una junta.
- Precondición: la junta existe.
- Flujo: el sistema abre un panel con el nombre de la junta y un formulario (Cédula, Nombres, Correo) → completa → clic en "Generar Cuenta y Asignar Rol" → el sistema crea el usuario ADMIN_JUNTA en esa junta, genera una contraseña aleatoria y muestra un resumen con las credenciales.
- Alterno: reasignar el rol a otra persona desde la misma fila; el sistema desactiva la cuenta administrativa anterior antes de crear la nueva.
- Excepción: si el correo ya está en uso por otra cuenta administrativa, se rechaza con mensaje de error.
- Reglas: máximo 2 cuentas ADMIN_JUNTA activas por junta; la cuenta nueva debe cambiar la contraseña en su primer inicio de sesión.

## CU03 Configurar datos generales
- Actor: Administrador de la Junta. Prioridad alta.
- Disparador: opción "Configuración de Junta" en el menú lateral.
- Flujo: el sistema muestra pestañas "Información General", "Ubicación" y "Sectores" → el administrador edita los datos (incluye logo y monto de multa por inasistencia) y agrega sectores con un campo de texto y el botón "+ Añadir" → clic en "Actualizar Datos" → el sistema guarda y confirma.
- Alterno: puede moverse entre pestañas sin perder lo escrito hasta que guarde o salga.
- Excepción: si un campo obligatorio (como Dirección) está vacío, el sistema lleva al campo y muestra advertencia.
- Reglas: los sectores registrados aquí alimentan los selectores de Secretaría y Tesorería.

## CU04 Administrar cuentas individuales
- Actor: Administrador de la Junta. Prioridad alta.
- Disparador: botón "Generar Accesos" en el módulo de Usuarios.
- Precondición: el Secretario ya registró al socio en la nómina (CU05).
- Flujo: el sistema lista los socios sin cuenta activa con un checkbox cada uno → selecciona uno o varios → clic en "Crear Credenciales (X seleccionados)" → el sistema usa la cédula como usuario, genera una clave genérica y marca "Estado de Cuenta" como "Activa" (badge verde).
- Alterno: checkbox de cabecera para seleccionar a todos los socios sin cuenta.
- Excepción: los socios INACTIVOS en la nómina tienen el checkbox bloqueado.
- Reglas: el socio debe cambiar la clave genérica en su primer inicio de sesión. El administrador también puede desactivar una cuenta; una cuenta desactivada no inicia sesión.

---
# Casos de uso agregados en la fase de especificación (no estaban en el APE MDS 3)

## CU20 Iniciar y cerrar sesión
- Actores: todos los usuarios. Prioridad alta.
- Disparador: abrir la aplicación sin sesión activa.
- Flujo: el sistema muestra el formulario de inicio de sesión (usuario y contraseña) → el usuario ingresa sus datos y presiona "Iniciar Sesión" → el sistema valida, identifica su rol y su junta, y lo lleva a su panel (CU25 para la directiva, CU17 para el socio, listado de juntas para el Super Administrador).
- Alterno: si la cuenta tiene `debeCambiarPassword`, el sistema lo lleva primero al CU22 y no le deja usar nada más hasta cambiarla.
- Excepciones: credenciales incorrectas muestran "Usuario o contraseña inválidos"; cuenta desactivada muestra "Cuenta desactivada, contacte a la directiva"; junta suspendida (CU24) muestra "La organización se encuentra suspendida".
- Reglas: tras 5 intentos fallidos seguidos, la cuenta se bloquea 15 minutos. "Cerrar Sesión" en la barra superior termina la sesión y vuelve al inicio.

## CU21 Recuperar contraseña
- Actores: cualquier usuario (solicita), Administrador de la Junta o Super Administrador (atiende). Prioridad alta.
- Disparador: enlace "¿Olvidaste tu contraseña?" en la pantalla de inicio de sesión.
- Flujo: el usuario ingresa su cédula o usuario → el sistema registra una solicitud de recuperación y muestra "Tu solicitud fue enviada a la directiva" → el Administrador de la Junta ve la solicitud en su panel, verifica a la persona y presiona "Restablecer" → el sistema genera una contraseña temporal, la muestra una sola vez al administrador y marca la cuenta con `debeCambiarPassword`.
- Alterno: si quien solicita es un Administrador de la Junta, la solicitud la atiende el Super Administrador.
- Excepción: si la cédula o usuario no existe, el sistema muestra el mismo mensaje de "solicitud enviada" para no revelar qué cuentas existen, pero no crea solicitud.
- Reglas: como no se envían correos, el administrador entrega la contraseña temporal en persona. Una solicitud atendida no se puede volver a usar.

## CU22 Cambiar contraseña
- Actores: todos los usuarios. Prioridad alta.
- Disparador: opción "Cambiar contraseña" en el menú del usuario, u obligatorio tras el primer ingreso o una recuperación.
- Flujo: el sistema pide contraseña actual, nueva y confirmación → el usuario las ingresa → el sistema valida y guarda → muestra confirmación y quita la marca `debeCambiarPassword`.
- Excepciones: contraseña actual incorrecta; nueva y confirmación distintas; nueva igual a la anterior.
- Reglas: mínimo 8 caracteres, con al menos una letra y un número.

## CU23 Gestionar cuentas de la directiva
- Actor: Administrador de la Junta. Prioridad alta.
- Disparador: pestaña "Directiva" en el módulo de Usuarios.
- Flujo: el sistema lista las cuentas de la directiva de su junta → presiona "Nueva cuenta" → ingresa cédula, nombres, apellidos, correo y rol (SECRETARIO o TESORERO) → el sistema valida, crea la cuenta con contraseña temporal mostrada una sola vez y `debeCambiarPassword` → confirma.
- Alterno: editar datos, cambiar el rol, desactivar o reactivar una cuenta.
- Excepciones: cédula, correo o usuario repetidos se rechazan.
- Reglas: el Administrador de la Junta no puede crear SUPER_ADMIN ni ADMIN_JUNTA (eso es del CU02). Una cuenta desactivada no inicia sesión. Una persona puede tener un solo rol.

## CU24 Editar o suspender juntas
- Actor: Super Administrador de la Plataforma. Prioridad media.
- Disparador: ícono de lápiz o botón "Suspender" en la fila de una junta.
- Flujo: editar abre el mismo modal del CU01 con los datos cargados → guarda → confirma. Suspender pide confirmación y un motivo → la junta pasa a `activa = false`.
- Alterno: "Reactivar" una junta suspendida.
- Excepción: el RUC editado no puede repetir el de otra junta.
- Reglas: los usuarios de una junta suspendida no pueden iniciar sesión, pero sus datos se conservan. Una junta no se elimina.

## CU25 Ver panel de inicio de la directiva
- Actores: Administrador de la Junta, Secretario/a, Tesorero/a. Prioridad media.
- Disparador: inicio de sesión de un miembro de la directiva.
- Flujo: el sistema muestra tarjetas según el rol: total de socios activos, próximos eventos y turnos, asistencias pendientes de cerrar, saldo actual, total de deuda por cobrar, ítems con stock bajo, mensajes no leídos y solicitudes de recuperación pendientes (estas solo para el administrador).
- Reglas: cada tarjeta lleva al módulo correspondiente; cada rol solo ve las tarjetas de lo que puede hacer.
