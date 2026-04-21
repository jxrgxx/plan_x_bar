<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['error' => 'Método no permitido'], 405);
}

$body = getBody();

// --- Datos del restaurante ---
$nombre    = trim($body['nombre'] ?? '');
$email     = trim($body['email'] ?? '');
$direccion = trim($body['direccion'] ?? '');
$telefono  = trim($body['telefono'] ?? '');

// --- Datos del admin ---
$admin_nombre   = trim($body['admin_nombre'] ?? '');
$admin_email    = trim($body['admin_email'] ?? '');
$admin_password = trim($body['admin_password'] ?? '');

// Validaciones
if (!$nombre || !$email || !$admin_nombre || !$admin_email || !$admin_password) {
    jsonResponse(['error' => 'Faltan campos obligatorios'], 400);
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    jsonResponse(['error' => 'Email del restaurante no válido'], 400);
}

if (!filter_var($admin_email, FILTER_VALIDATE_EMAIL)) {
    jsonResponse(['error' => 'Email del administrador no válido'], 400);
}


$db = getDB();

// Comprobar que el email del restaurante no exista
$stmt = $db->prepare("SELECT id FROM Restaurante WHERE email = ?");
$stmt->execute([$email]);
if ($stmt->fetch()) {
    jsonResponse(['error' => 'Ya existe un restaurante con ese email'], 409);
}

// Comprobar que el email del admin no exista en Trabajadores
$stmt = $db->prepare("SELECT id FROM Trabajadores WHERE email = ?");
$stmt->execute([$admin_email]);
if ($stmt->fetch()) {
    jsonResponse(['error' => 'Ya existe un trabajador con ese email'], 409);
}

// Insertar restaurante
$stmt = $db->prepare("INSERT INTO Restaurante (nombre, email, direccion, telefono) VALUES (?, ?, ?, ?)");
$stmt->execute([$nombre, $email, $direccion, $telefono]);
$restaurante_id = $db->lastInsertId();

// Insertar admin con password hasheado
$password_hash = password_hash($admin_password, PASSWORD_BCRYPT);
$stmt = $db->prepare("INSERT INTO Trabajadores (restaurante_id, nombre, rol, email, password_hash) VALUES (?, ?, 'admin', ?, ?)");
$stmt->execute([$restaurante_id, $admin_nombre, $admin_email, $password_hash]);
$trabajador_id = $db->lastInsertId();

jsonResponse([
    'success'        => true,
    'restaurante_id' => (int) $restaurante_id,
    'trabajador'     => [
        'id'                  => (int) $trabajador_id,
        'restaurante_id'      => (int) $restaurante_id,
        'nombre'              => $admin_nombre,
        'rol'                 => 'admin',
        'email'               => $admin_email,
        'restaurante_nombre'  => $nombre,
    ]
], 201);
