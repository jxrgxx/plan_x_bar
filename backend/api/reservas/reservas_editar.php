<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['success' => false, 'error' => 'Método no permitido'], 405);

$body   = getBody();
$id     = (int)($body['id'] ?? 0);
$estado = trim($body['estado'] ?? '');

$estadosValidos = ['pendiente', 'confirmada', 'cancelada', 'completada'];
if (!$id || !in_array($estado, $estadosValidos)) {
    jsonResponse(['success' => false, 'error' => 'Datos inválidos']);
}

try {
    $db   = getDB();
    $stmt = $db->prepare("UPDATE Reservas SET estado = ? WHERE id = ?");
    $stmt->execute([$estado, $id]);
    jsonResponse(['success' => true]);
} catch (Exception $e) {
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
