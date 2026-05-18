<?php
require_once 'config/db.php';

$db = getDB();

// 1. Añadir columna password_hash si no existe
try {
    $db->exec("ALTER TABLE Trabajadores ADD COLUMN password_hash VARCHAR(255) NULL DEFAULT NULL");
    echo "Columna password_hash añadida.<br>";
} catch (Exception $e) {
    echo "Columna ya existe (ok).<br>";
}

// 2. Poner contraseña 1212 solo al admin
$hash = password_hash('1212', PASSWORD_BCRYPT);
$stmt = $db->prepare("UPDATE Trabajadores SET password_hash = ? WHERE rol = 'admin'");
$stmt->execute([$hash]);

echo "Contraseña del admin actualizada a 1212. Borra este archivo del servidor.";
