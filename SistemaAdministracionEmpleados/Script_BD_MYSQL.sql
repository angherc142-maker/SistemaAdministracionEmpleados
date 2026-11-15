-- =====================================================================
-- Sistema de Administración de Empleados - Módulo 3 (persistencia)
-- Script RESTAURABLE de la base de datos para MySQL 8
-- Crea la base, las tablas y carga datos de prueba.
--
-- ATENCIÓN: este script BORRA las tablas empleados, remuneraciones y
-- remuneracion_valores (si existen) y las vuelve a crear con los datos
-- de prueba. Úsalo para restaurar la base desde cero. La tabla usuarios
-- NO se borra; si está vacía, la aplicación crea el usuario admin / 1234
-- (contraseña cifrada) al iniciar sesión.
--
-- Cómo usarlo en MySQL Workbench:
--   1) Cierra la aplicación.
--   2) Abre "Local instance MySQL80" e ingresa tu contraseña.
--   3) File > Open SQL Script... y elige este archivo.
--   4) Ejecuta todo con Execute all (Ctrl + Shift + Enter).
--
-- Formatos que valida la aplicación:
--   código    : 2 letras y 3 números (ej. EM001)
--   documento : 8 dígitos, guion y dígito verificador (ej. 08011990-1)
--   nombre    : solo letras y espacios
--
-- Nota: la aplicación también crea la base y las tablas sola al iniciar
-- (clases EsquemaBD y EmpleadoService.prepararEsquema); este script es el
-- respaldo restaurable y usa los mismos nombres, tipos y restricciones.
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS remuneraciones_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE remuneraciones_db;

-- Se borran primero las tablas hijas (por las llaves foráneas)
DROP TABLE IF EXISTS remuneracion_valores;
DROP TABLE IF EXISTS remuneraciones;
DROP TABLE IF EXISTS empleados;

-- ---------------------------------------------------------------------
-- Usuarios del sistema (contraseña cifrada con PBKDF2, formato
-- iteraciones:sal:hash). No se siembra aquí: la app crea admin / 1234.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  usuario     VARCHAR(50)  NOT NULL,
  clave_hash  VARCHAR(200) NOT NULL,
  UNIQUE KEY uq_usuarios_usuario (usuario)
);

-- ---------------------------------------------------------------------
-- Empleados
--   tipo (igual que en EmpleadoService): 'Administrativo' | 'Por hora' | 'Por comisión'
--   salario_base aplica solo a Administrativo (NULL en los demás tipos);
--   tarifa_hora y horas_trabajadas aplican a 'Por hora';
--   monto_ventas y porcentaje_comision aplican a 'Por comisión'.
--   Las columnas que no aplican a un tipo quedan en NULL.
--   Las restricciones CHECK repiten las validaciones de las clases Java.
-- ---------------------------------------------------------------------
CREATE TABLE empleados (
  id                   INT AUTO_INCREMENT PRIMARY KEY,
  codigo               VARCHAR(30)
                       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  cedula               VARCHAR(30)   NOT NULL UNIQUE,
  nombre               VARCHAR(100)  NOT NULL,
  cargo                VARCHAR(100),
  salario_base         DECIMAL(12,2) NULL,
  fecha_ingreso        DATE,
  activo               BOOLEAN       NOT NULL DEFAULT TRUE,
  tipo                 VARCHAR(20)   NULL,
  tarifa_hora          DECIMAL(12,2),
  horas_trabajadas     DECIMAL(8,2),
  monto_ventas         DECIMAL(12,2),
  porcentaje_comision  DECIMAL(5,2),
  UNIQUE KEY uq_empleados_codigo (codigo),
  CONSTRAINT ck_empleados_tipo CHECK (tipo IN ('Administrativo', 'Por hora', 'Por comisión')),
  CONSTRAINT chk_salario_base CHECK (salario_base IS NULL OR salario_base > 0),
  CONSTRAINT chk_tarifa_hora CHECK (tarifa_hora IS NULL OR tarifa_hora > 0),
  CONSTRAINT chk_horas_trabajadas CHECK (horas_trabajadas IS NULL OR (horas_trabajadas > 0 AND horas_trabajadas <= 744)),
  CONSTRAINT chk_monto_ventas CHECK (monto_ventas IS NULL OR monto_ventas >= 0),
  CONSTRAINT chk_porcentaje_comision CHECK (porcentaje_comision IS NULL OR (porcentaje_comision > 0 AND porcentaje_comision <= 100))
);

-- ---------------------------------------------------------------------
-- Historial de remuneraciones (un registro por empleado y período)
-- ---------------------------------------------------------------------
CREATE TABLE remuneraciones (
  id                 INT AUTO_INCREMENT PRIMARY KEY,
  empleado_id        INT           NOT NULL,
  periodo            CHAR(7)       NOT NULL,            -- formato AAAA-MM, ej. 2026-11
  importe_calculado  DECIMAL(12,2) NOT NULL,
  fecha_calculo      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_empleado_periodo (empleado_id, periodo),
  FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);

-- ---------------------------------------------------------------------
-- Valores usados en cada cálculo. Conceptos que guarda la app:
--   Administrativo: salarioBase
--   Por hora:       tarifaPorHora, horasTrabajadas
--   Por comisión:   montoVentas, porcentajeComision
-- ---------------------------------------------------------------------
CREATE TABLE remuneracion_valores (
  remuneracion_id  INT           NOT NULL,
  concepto         VARCHAR(50)   NOT NULL,
  monto            DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (remuneracion_id, concepto),
  FOREIGN KEY (remuneracion_id) REFERENCES remuneraciones(id) ON DELETE CASCADE
);

-- =====================================================================
-- DATOS DE PRUEBA (importes calculados como lo hace la aplicación)
-- =====================================================================

INSERT INTO empleados
  (id, codigo, cedula, nombre, cargo, salario_base, fecha_ingreso, activo, tipo,
   tarifa_hora, horas_trabajadas, monto_ventas, porcentaje_comision)
VALUES
  (1, 'EM001', '08011990-1', 'Ana Pérez',       'Analista',           800.00, '2023-03-15', TRUE,  'Administrativo', NULL, NULL,   NULL,     NULL),
  (2, 'EM002', '08011992-2', 'Luis Martínez',   'Técnico de soporte',   NULL, '2024-01-08', TRUE,  'Por hora',       6.50, 160.00, NULL,     NULL),
  (3, 'EM003', '08011995-3', 'Marta Gómez',     'Vendedora',            NULL, '2022-06-20', TRUE,  'Por comisión',   NULL, NULL,   12500.00, 5.00),
  (4, 'EM004', '08011988-4', 'Pedro Ramírez',   'Chofer',             500.00, '2021-11-02', TRUE,  'Administrativo', NULL, NULL,   NULL,     NULL),
  (5, 'EM005', '08011985-5', 'Rosa Hernández',  'Contadora',          900.00, '2020-08-01', FALSE, 'Administrativo', NULL, NULL,   NULL,     NULL),
  (6, 'EM006', '08011991-6', 'Sofía Castillo',  'Cajera',               NULL, '2023-09-10', TRUE,  'Por hora',       5.25, 120.00, NULL,     NULL),
  (7, 'EM007', '08011997-7', 'Diego Alvarado',  'Ejecutivo de ventas',  NULL, '2025-02-17', TRUE,  'Por comisión',   NULL, NULL,   8000.00,  4.50);

INSERT INTO remuneraciones (id, empleado_id, periodo, importe_calculado, fecha_calculo) VALUES
  (1, 1, '2026-10',  800.00, '2026-10-31 18:00:00'),   -- Ana: salario base
  (2, 1, '2026-11',  800.00, '2026-11-04 22:20:00'),   -- Ana: salario base
  (3, 2, '2026-11', 1040.00, '2026-11-05 09:00:00'),   -- Luis: 6.50 x 160 horas
  (4, 3, '2026-11',  625.00, '2026-11-05 09:05:00'),   -- Marta: 12500 x 5%
  (5, 4, '2026-11',  500.00, '2026-11-05 09:10:00'),   -- Pedro: salario base
  (6, 5, '2026-10',  900.00, '2026-10-31 18:05:00'),   -- Rosa: salario base
  (7, 6, '2026-11',  630.00, '2026-11-05 09:15:00'),   -- Sofía: 5.25 x 120 horas
  (8, 7, '2026-11',  360.00, '2026-11-05 09:20:00');   -- Diego: 8000 x 4.5 %

INSERT INTO remuneracion_valores (remuneracion_id, concepto, monto) VALUES
  (1, 'salarioBase',        800.00),
  (2, 'salarioBase',        800.00),
  (3, 'tarifaPorHora',        6.50), (3, 'horasTrabajadas',   160.00),
  (4, 'montoVentas',      12500.00), (4, 'porcentajeComision',  5.00),
  (5, 'salarioBase',        500.00),
  (6, 'salarioBase',        900.00),
  (7, 'tarifaPorHora',        5.25), (7, 'horasTrabajadas',   120.00),
  (8, 'montoVentas',       8000.00), (8, 'porcentajeComision',  4.50);

-- =====================================================================
-- COMPROBACIÓN (debe dar: 7 empleados, 8 remuneraciones, 12 valores)
-- =====================================================================
SELECT (SELECT COUNT(*) FROM empleados)            AS empleados,
       (SELECT COUNT(*) FROM remuneraciones)       AS remuneraciones,
       (SELECT COUNT(*) FROM remuneracion_valores) AS valores;

SELECT e.codigo, e.nombre, e.tipo, r.periodo, r.importe_calculado
FROM empleados e
JOIN remuneraciones r ON r.empleado_id = e.id
ORDER BY e.id, r.periodo;