<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['error' => 'Método no permitido'], 405);
}

$body   = getBody();
$id     = intval($body['id'] ?? 0);
$estado = $body['estado'] ?? '';

$permitidos = ['libre', 'ocupada', 'reservada'];
if (!$id || !in_array($estado, $permitidos)) {
    jsonResponse(['error' => 'Datos inválidos'], 400);
}

$db   = getDB();
$stmt = $db->prepare("UPDATE Mesas SET estado = ? WHERE id = ?");
$stmt->execute([$estado, $id]);

jsonResponse(['success' => true]);
