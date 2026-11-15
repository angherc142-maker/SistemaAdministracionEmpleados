-- =====================================================================
-- Sistema de Administración de Empleados - Módulo 3 (persistencia)
-- Script RESTAURABLE de la base de datos para MySQL 8
-- Crea la base, las 3 tablas y carga datos de prueba.
--
-- ATENCIÓN: este script BORRA las tablas empleados, remuneraciones y
-- remuneracion_valores (si existen) y las vuelve a crear con los datos
-- de prueba. Úsalo para restaurar la base desde cero.
--
-- Cómo usarlo en MySQL Workbench:
--   1) Abre "Local instance MySQL80" e ingresa tu contraseña.
--   2) File > Open SQL Script... y elige este archivo.
--   3) Ejecuta todo con Execute all (Ctrl + Shift + Enter).
--
-- Nota: la aplicación también crea la base y las tablas sola al iniciar
-- (clase EsquemaBD); este script es el respaldo restaurable.
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
-- Empleados
--   tipo: ASALARIADO | POR_HORAS | COMISION
--   tarifa_hora y horas_trabajadas aplican a POR_HORAS;
--   monto_ventas y porcentaje_comision aplican a COMISION.
--   Las columnas que no aplican a un tipo quedan en NULL.
-- ---------------------------------------------------------------------
CREATE TABLE empleados (
  id                   INT AUTO_INCREMENT PRIMARY KEY,
  codigo               VARCHAR(20)   UNIQUE,
  cedula               VARCHAR(30)   NOT NULL UNIQUE,
  nombre               VARCHAR(100)  NOT NULL,
  cargo                VARCHAR(100),
  salario_base         DECIMAL(12,2) NOT NULL,
  fecha_ingreso        DATE,
  activo               BOOLEAN       NOT NULL DEFAULT TRUE,
  tipo                 VARCHAR(20)   NOT NULL DEFAULT 'ASALARIADO',
  tarifa_hora          DECIMAL(10,2),
  horas_trabajadas     DECIMAL(8,2),
  monto_ventas         DECIMAL(12,2),
  porcentaje_comision  DECIMAL(5,2),
  CONSTRAINT ck_empleados_tipo CHECK (tipo IN ('ASALARIADO', 'POR_HORAS', 'COMISION'))
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
-- Valores usados en cada cálculo (salarioBase, horasExtra, deducciones...)
-- ---------------------------------------------------------------------
CREATE TABLE remuneracion_valores (
  remuneracion_id  INT           NOT NULL,
  concepto         VARCHAR(50)   NOT NULL,
  monto            DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (remuneracion_id, concepto),
  FOREIGN KEY (remuneracion_id) REFERENCES remuneraciones(id) ON DELETE CASCADE
);

-- =====================================================================
-- DATOS DE PRUEBA
-- =====================================================================

INSERT INTO empleados
  (id, codigo, cedula, nombre, cargo, salario_base, fecha_ingreso, activo, tipo,
   tarifa_hora, horas_trabajadas, monto_ventas, porcentaje_comision)
VALUES
  (1, 'EMP-001', '0801-1990-00001', 'Ana Pérez',       'Analista',           800.00, '2023-03-15', TRUE,  'ASALARIADO', NULL, NULL,   NULL,     NULL),
  (2, 'EMP-002', '0801-1992-00002', 'Luis Martínez',   'Técnico de soporte',   0.00, '2024-01-08', TRUE,  'POR_HORAS',  6.50, 160.00, NULL,     NULL),
  (3, 'EMP-003', '0801-1995-00003', 'Marta Gómez',     'Vendedora',          400.00, '2022-06-20', TRUE,  'COMISION',   NULL, NULL,   12500.00, 5.00),
  (4, 'EMP-004', '0801-1988-00004', 'Pedro Ramírez',   'Chofer',             500.00, '2021-11-02', TRUE,  'ASALARIADO', NULL, NULL,   NULL,     NULL),
  (5, 'EMP-005', '0801-1985-00005', 'Rosa Hernández',  'Contadora',          900.00, '2020-08-01', FALSE, 'ASALARIADO', NULL, NULL,   NULL,     NULL);

INSERT INTO remuneraciones (id, empleado_id, periodo, importe_calculado, fecha_calculo) VALUES
  (1, 1, '2026-10',  770.00, '2026-10-31 18:00:00'),
  (2, 1, '2026-11',  830.00, '2026-11-04 22:20:00'),
  (3, 2, '2026-11',  936.00, '2026-11-05 09:00:00'),
  (4, 3, '2026-11',  975.00, '2026-11-05 09:05:00'),
  (5, 4, '2026-11',  485.00, '2026-11-05 09:10:00'),
  (6, 5, '2026-10',  830.00, '2026-10-31 18:05:00');

INSERT INTO remuneracion_valores (remuneracion_id, concepto, monto) VALUES
  (1, 'salarioBase',   800.00), (1, 'horasExtra',      60.00), (1, 'deducciones',  90.00),
  (2, 'salarioBase',   800.00), (2, 'horasExtra',     120.00), (2, 'deducciones',  90.00),
  (3, 'tarifaHora',      6.50), (3, 'horasTrabajadas', 160.00), (3, 'deducciones', 104.00),
  (4, 'salarioBase',   400.00), (4, 'comision',       625.00), (4, 'deducciones',  50.00),
  (5, 'salarioBase',   500.00), (5, 'bonos',           25.00), (5, 'deducciones',  40.00),
  (6, 'salarioBase',   900.00), (6, 'deducciones',     70.00);

-- =====================================================================
-- COMPROBACIÓN (debe dar: 5 empleados, 6 remuneraciones, 17 valores)
-- =====================================================================
SELECT (SELECT COUNT(*) FROM empleados)           AS empleados,
       (SELECT COUNT(*) FROM remuneraciones)      AS remuneraciones,
       (SELECT COUNT(*) FROM remuneracion_valores) AS valores;

SELECT e.codigo, e.nombre, e.tipo, r.periodo, r.importe_calculado
FROM empleados e
JOIN remuneraciones r ON r.empleado_id = e.id
ORDER BY e.id, r.periodo;
