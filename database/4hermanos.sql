CREATE DATABASE IF NOT EXISTS 4hermanos CHARACTER SET utf8mb4;
USE 4hermanos;

CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(120) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    contrasena CHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS saldos_usuarios (
    id INT PRIMARY KEY,
    nombre VARCHAR(100),
    correo VARCHAR(120),
    telefono VARCHAR(20),
    dinero DECIMAL(12,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (id) REFERENCES usuarios(id)
);

-- Usuario de prueba: demo@casino.com / demo1234 (la contraseña se guarda como SHA-256)
INSERT INTO usuarios (nombre, correo, telefono, contrasena)
VALUES ('Usuario Demo', 'demo@casino.com', '5550000000', '0ead2060b65992dca4769af601a1b3a35ef38cfad2c2c465bb160ea764157c5d');
