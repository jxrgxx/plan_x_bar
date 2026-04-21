<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body           = getBody();
$restaurante_id = (int)($body['restaurante_id'] ?? 1);
$nombre         = trim($body['nombre'] ?? '');
$rol            = trim($body['rol'] ?? '');
$email          = trim($body['email'] ?? '');
$password       = trim($body['password'] ?? '');

$roles = ['admin', 'camarero', 'cocina'];
if (!$nombre || !in_array($rol, $roles) || !$email || !$password) {
    jsonResponse(['error' => 'Todos los campos son obligatorios'], 400);
}
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) jsonResponse(['error' => 'Email no válido'], 400);
if (strlen($password) < 6) jsonResponse(['error' => 'La contraseña debe tener al menos 6 caracteres'], 400);

$db = getDB();

$stmt = $db->prepare("SELECT id FROM Trabajadores WHERE email = ?");
$stmt->execute([$email]);
if ($stmt->fetch()) jsonResponse(['error' => 'Ya existe un trabajador con ese email'], 409);

$hash = password_hash($password, PASSWORD_BCRYPT);
$stmt = $db->prepare("INSERT INTO Trabajadores (restaurante_id, nombre, rol, email, password_hash) VALUES (?, ?, ?, ?, ?)");
$stmt->execute([$restaurante_id, $nombre, $rol, $email, $hash]);

jsonResponse(['success' => true, 'id' => (int)$db->lastInsertId()], 201);
