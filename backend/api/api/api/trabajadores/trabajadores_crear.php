<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['success' => false, 'error' => 'Método no permitido'], 405);

$body           = getBody();
$restaurante_id = (int)($body['restaurante_id'] ?? 0);
$nombre         = trim($body['nombre'] ?? '');
$rol            = trim($body['rol'] ?? '');
$email          = trim($body['email'] ?? '');
$password       = trim($body['password'] ?? '');

$roles = ['admin', 'camarero', 'cocina'];
if (!$restaurante_id || !$nombre || !in_array($rol, $roles) || !$email || !$password) {
    jsonResponse(['success' => false, 'error' => 'Todos los campos son obligatorios']);
}
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    jsonResponse(['success' => false, 'error' => 'Email no válido']);
}

try {
    $db = getDB();

    $stmt = $db->prepare("SELECT id FROM Trabajadores WHERE email = ?");
    $stmt->execute([$email]);
    if ($stmt->fetch()) {
        jsonResponse(['success' => false, 'error' => 'Ya existe un trabajador con ese email']);
    }

    $hash = password_hash($password, PASSWORD_BCRYPT);
    $stmt = $db->prepare("INSERT INTO Trabajadores (restaurante_id, nombre, rol, email, password_hash) VALUES (?, ?, ?, ?, ?)");
    $stmt->execute([$restaurante_id, $nombre, $rol, $email, $hash]);

    jsonResponse(['success' => true, 'id' => (int)$db->lastInsertId()]);
} catch (Exception $e) {
    jsonResponse(['success' => false, 'error' => $e->getMessage()]);
}
