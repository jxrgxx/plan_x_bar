<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['error' => 'Método no permitido'], 405);
}

$body = getBody();
$email    = trim($body['email'] ?? '');
$password = trim($body['password'] ?? '');

if (!$email || !$password) {
    jsonResponse(['error' => 'Email y contraseña requeridos'], 400);
}

$db   = getDB();
$stmt = $db->prepare("
    SELECT t.id, t.restaurante_id, t.nombre, t.rol, t.email, t.password_hash, t.activo,
           r.nombre AS restaurante_nombre
    FROM Trabajadores t
    JOIN Restaurante r ON r.id = t.restaurante_id
    WHERE t.email = ?
");
$stmt->execute([$email]);
$user = $stmt->fetch();

if (!$user || !$user['activo'] || !password_verify($password, $user['password_hash'])) {
    jsonResponse(['error' => 'Credenciales incorrectas'], 401);
}

unset($user['password_hash'], $user['activo']);
$user['id']             = (int) $user['id'];
$user['restaurante_id'] = (int) $user['restaurante_id'];
jsonResponse(['success' => true, 'trabajador' => $user]);
