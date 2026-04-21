<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body     = getBody();
$id       = (int)($body['id'] ?? 0);
$nombre   = trim($body['nombre'] ?? '');
$rol      = trim($body['rol'] ?? '');
$email    = trim($body['email'] ?? '');
$activo   = isset($body['activo']) ? (bool)$body['activo'] : true;
$password = trim($body['password'] ?? '');

$roles = ['admin', 'camarero', 'cocina'];
if (!$id || !$nombre || !in_array($rol, $roles) || !$email) {
    jsonResponse(['error' => 'Datos inválidos'], 400);
}

$db = getDB();

$stmt = $db->prepare("SELECT id FROM Trabajadores WHERE email = ? AND id != ?");
$stmt->execute([$email, $id]);
if ($stmt->fetch()) jsonResponse(['error' => 'Ese email ya lo usa otro trabajador'], 409);

if ($password) {
$hash = password_hash($password, PASSWORD_BCRYPT);
    $stmt = $db->prepare("UPDATE Trabajadores SET nombre = ?, rol = ?, email = ?, activo = ?, password_hash = ? WHERE id = ?");
    $stmt->execute([$nombre, $rol, $email, $activo, $hash, $id]);
} else {
    $stmt = $db->prepare("UPDATE Trabajadores SET nombre = ?, rol = ?, email = ?, activo = ? WHERE id = ?");
    $stmt->execute([$nombre, $rol, $email, $activo, $id]);
}

jsonResponse(['success' => true]);
