# Estructura del proyecto

Arquitectura en capas tomada del diagrama de paquetes del equipo:

```
rest → service → repository → model
rest → model
service → model
```

Paquete base: `ec.uta.multijunta`

```
ec.uta.multijunta
├── model        entidades JPA y enums
├── repository   interfaces Spring Data JPA
├── service      reglas de negocio y validaciones
├── rest         controladores (@Controller para Thymeleaf)
└── config       seguridad y carga de datos de prueba
src/main/resources/templates/<modulo>/   vistas Thymeleaf
```

## Componentes (del diagrama de componentes)
- Usuario y Autenticación (expone IAutenticacion)
- Gestión de Juntas (usa IAutenticacion)
- Gestión Administrativa (usa IAutenticacion, expone IAsistencia)
- Gestión Financiera (usa IAutenticacion e IAsistencia)
- Gestión de Comunicaciones (usa IAutenticacion)

## Despliegue
Un solo servidor con la aplicación y la base de datos. Clientes: navegador del computador de la directiva y navegador del celular de los socios. La app móvil (.apk) del diagrama de despliegue queda fuera de este alcance y se reemplaza por la interfaz responsive.
