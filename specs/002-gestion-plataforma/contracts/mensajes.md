# Contrato: mensajes exactos (002)

Claves para `messages.properties`. Los textos son los de la spec.

| Clave | Texto | CU |
|---|---|---|
| junta.registrada | Junta registrada con éxito | CU01 |
| junta.actualizada | Junta actualizada con éxito | CU24 |
| junta.suspendida | Junta suspendida | CU24 |
| junta.reactivada | Junta reactivada | CU24 |
| junta.rucDuplicado | Este RUC ya está registrado | CU01, CU24 |
| junta.rucFormato | El RUC debe tener 13 dígitos | CU01 |
| junta.cantonInvalido | El cantón no pertenece a la provincia elegida | CU01 |
| junta.motivoObligatorio | Indique el motivo de la suspensión | CU24 |
| junta.motivoLargo | El motivo admite hasta 300 caracteres | CU24 |
| campo.obligatorio | Este campo es obligatorio | todos |
| admin.limite | La junta ya tiene 2 administradores activos; use Reasignar | CU02 |
| admin.creado | Cuenta de administrador creada. Entregue estas credenciales en persona. | CU02 |
| cuenta.correoDuplicado | Este correo ya está en uso por otra cuenta administrativa | CU02, CU23 |
| cuenta.cedulaDuplicada | Esta cédula ya está registrada en la plataforma | CU02, CU04, CU23 |
| cuenta.cedulaFormato | La cédula debe tener 10 dígitos | CU02, CU23 |
| configuracion.actualizada | Datos de la junta actualizados | CU03 |
| configuracion.direccionObligatoria | La dirección es obligatoria | CU03 |
| configuracion.logoInvalido | El logo debe ser una imagen PNG o JPG de hasta 5 MB | CU03 |
| configuracion.montoInvalido | Ingrese un monto válido (0 o mayor, hasta 2 decimales) | CU03 |
| sector.duplicado | Ese sector ya existe | CU03 |
| sector.vacio | Escriba el nombre del sector | CU03 |
| sector.largo | El nombre del sector admite hasta 80 caracteres | CU03 |
| sector.conSocios | No se puede quitar un sector con socios asignados | CU03 |
| sector.ninguno | Registre al menos un sector para poder inscribir socios | CU03 |
| socios.seleccionVacia | Seleccione al menos un socio | CU04 |
| socios.desactivada | Cuenta del socio desactivada | CU04 |
| socios.reactivada | Cuenta del socio reactivada | CU04 |
| directiva.creada | Cuenta creada. Entregue la contraseña temporal en persona. | CU23 |
| directiva.actualizada | Cuenta actualizada | CU23 |
| directiva.desactivada | Cuenta desactivada | CU23 |
| directiva.reactivada | Cuenta reactivada | CU23 |
| panel.proximamente | Disponible próximamente | CU25 |
