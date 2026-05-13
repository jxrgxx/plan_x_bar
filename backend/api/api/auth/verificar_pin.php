<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['error' => 'Método no permitido'], 405);
}

$body = getBody();
$trabajador_id = (int) ($body['trabajador_id'] ?? 0);
$pin           = trim($body['pin'] ?? '');

if (!$trabajador_id || strlen($pin) !== 4 || !ctype_digit($pin)) {
    jsonResponse(['error' => 'Datos inválidos'], 400);
}

$db   = getDB();
$stmt = $db->prepare("SELECT pin FROM Trabajadores WHERE id = ?");
$stmt->execute([$trabajador_id]);
$row  = $stmt->fetch();

if (!$row) {
    jsonResponse(['success' => false, 'error' => 'Trabajador no encontrado'], 404);
}

if (!$row['pin'] || !password_verify($pin, $row['pin'])) {
    jsonResponse(['success' => false, 'error' => 'PIN incorrecto']);
}

jsonResponse(['success' => true]);
