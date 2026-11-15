# Sistema  de Administración de Empleados

Aplicación de escritorio Java que permite gestionar empleados, calcular su remuneración según el tipo de contrato y consultar reportes.

| Integrante | Módulo |
|---|---|
| Ángel Gabriel Hércules Hércules | Gestión de empleados y coordinación |
| Tania Pamela Elías Vega  | Tipos de empleados y remuneraciones |
| Roberto Valentín Fernández Leiva | Base de datos y registro histórico |
| Kevin Javier Vásquez Girón | Interfaz gráfica, login y reportes |

## Tecnologías

- Java 21
- Java Swing (interfaz gráfica)
- MySQL 8 (JDBC)
- Maven
- Git y GitHub

## Funcionalidades

- Inicio de sesión
- Registro, búsqueda, modificación e inactivación de empleados
- Empleados administrativos, por hora y por comisión
- Cálculo de remuneraciones con desglose de valores
- Historial de remuneraciones
- Reportes con filtro por tipo de empleado y resumen (total, cantidad y promedio)

## Requisitos

- JDK 21 o superior
- MySQL Server 8.0
- Maven (o un IDE con soporte Maven, como VS Code o NetBeans)

## Instalación

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/angherc142-maker/SistemaAdministracionEmpleados.git
   ```
2. Crear la base de datos ejecutando `crear_base_de_datos.sql` en MySQL Workbench.
3. Configurar las credenciales de MySQL con variables de entorno:
   ```powershell
   $env:DB_USER = "root"
   $env:DB_PASS = "su_contraseña"
   $env:MYSQL_USER = "root"
   $env:MYSQL_PASSWORD = "su_contraseña"
   ```
4. Ejecutar la clase `LoginFrame` (paquete `com.sistemaempleados.vista`).

## Acceso inicial

- Usuario: `admin`
- Contraseña: `1234`

## Estructura del proyecto

```
src/main/java/com/sistemaempleados/
├── modelo/       Clases de empleados y cálculo de remuneraciones
├── mysql/        Conexión y persistencia en MySQL
├── servicio/     Lógica de negocio
└── vista/        Interfaz gráfica (Swing)
```

