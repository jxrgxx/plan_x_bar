<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['success' => false, 'error' => 'Método no permitido'], 405);
}

$body = getBody();
$trabajador_id = (int) ($body['trabajador_id'] ?? 0);
$pin           = trim($body['pin'] ?? '');

if (!$trabajador_id || strlen($pin) !== 4 || !ctype_digit($pin)) {
    jsonResponse(['success' => false, 'error' => 'Datos inválidos: id=' . $trabajador_id . ' pin=' . $pin]);
}

try {
    $db   = getDB();
    $hash = password_hash($pin, PASSWORD_DEFAULT);
    $stmt = $db->prepare("UPDATE Trabajadores SET pin = ? WHERE id = ?");
    $stmt->execute([$hash, $trabajador_id]);

    if ($stmt->rowCount() === 0) {
        jsonResponse(['success' => false, 'error' => 'Trabajador no encontrado (id=' . $trabajador_id . ')']);
    }

    jsonResponse(['success' => true]);
} catch (Exception $e) {
    jsonResponse(['success' => false, 'error' => 'Error BD: ' . $e->getMessage()]);
}
