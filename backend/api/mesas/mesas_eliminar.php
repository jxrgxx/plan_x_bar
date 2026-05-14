<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body = getBody();
$id   = (int)($body['id'] ?? 0);

if (!$id) jsonResponse(['error' => 'ID requerido'], 400);

$db = getDB();

$stmt = $db->prepare("SELECT estado FROM Mesas WHERE id = ?");
$stmt->execute([$id]);
$mesa = $stmt->fetch();
if (!$mesa) jsonResponse(['error' => 'Mesa no encontrada'], 404);
if ($mesa['estado'] === 'ocupada') jsonResponse(['error' => 'No se puede eliminar una mesa ocupada'], 409);

$db->prepare("DELETE FROM Mesas WHERE id = ?")->execute([$id]);

jsonResponse(['success' => true]);
