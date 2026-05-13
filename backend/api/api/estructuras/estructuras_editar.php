<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body   = getBody();
$id     = (int) ($body['id'] ?? 0);
$nombre = trim($body['nombre'] ?? '');
$posX   = (float) ($body['posX'] ?? 0);
$posY   = (float) ($body['posY'] ?? 0);
$ancho  = (float) ($body['ancho'] ?? 200);
$alto   = (float) ($body['alto'] ?? 150);
$color  = trim($body['color'] ?? '#BBDEFB');

if (!$id || !$nombre) jsonResponse(['success' => false, 'error' => 'Datos incompletos'], 400);

try {
    $db   = getDB();
    $stmt = $db->prepare("UPDATE Estructuras SET nombre=?, posX=?, posY=?, ancho=?, alto=?, color=? WHERE id=?");
    $stmt->execute([$nombre, $posX, $posY, $ancho, $alto, $color, $id]);
    jsonResponse(['success' => true]);
} catch (Exception $e) {
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
