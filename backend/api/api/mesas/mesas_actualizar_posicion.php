<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body = getBody();
$id   = (int)($body['id'] ?? 0);
$posX = (float)($body['posX'] ?? 0);
$posY = (float)($body['posY'] ?? 0);

if (!$id) jsonResponse(['error' => 'ID requerido'], 400);

$db   = getDB();
$stmt = $db->prepare("UPDATE Mesas SET posX = ?, posY = ? WHERE id = ?");
$stmt->execute([$posX, $posY, $id]);

jsonResponse(['success' => true]);
