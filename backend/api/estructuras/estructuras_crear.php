<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body           = getBody();
$restaurante_id = (int) ($body['restaurante_id'] ?? 0);
$nombre         = trim($body['nombre'] ?? '');
$posX           = (float) ($body['posX'] ?? 0);
$posY           = (float) ($body['posY'] ?? 0);
$ancho          = (float) ($body['ancho'] ?? 200);
$alto           = (float) ($body['alto'] ?? 150);
$color          = trim($body['color'] ?? '#BBDEFB');

if (!$restaurante_id || !$nombre) jsonResponse(['success' => false, 'error' => 'Datos incompletos'], 400);

try {
    $db   = getDB();
    $stmt = $db->prepare("INSERT INTO Estructuras (restaurante_id, nombre, posX, posY, ancho, alto, color) VALUES (?,?,?,?,?,?,?)");
    $stmt->execute([$restaurante_id, $nombre, $posX, $posY, $ancho, $alto, $color]);
    jsonResponse(['success' => true, 'id' => (int) $db->lastInsertId()]);
} catch (Exception $e) {
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
