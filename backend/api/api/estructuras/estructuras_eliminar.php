<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body = getBody();
$id   = (int) ($body['id'] ?? 0);

if (!$id) jsonResponse(['success' => false, 'error' => 'id requerido'], 400);

try {
    $db   = getDB();
    $stmt = $db->prepare("DELETE FROM Estructuras WHERE id = ?");
    $stmt->execute([$id]);
    jsonResponse(['success' => true]);
} catch (Exception $e) {
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
