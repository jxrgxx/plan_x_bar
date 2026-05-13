<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body = getBody();
$id   = (int)($body['id'] ?? 0);

if (!$id) jsonResponse(['error' => 'ID requerido'], 400);

$db = getDB();

$stmt = $db->prepare("SELECT rol FROM Trabajadores WHERE id = ?");
$stmt->execute([$id]);
$t = $stmt->fetch();
if (!$t) jsonResponse(['error' => 'Trabajador no encontrado'], 404);
if ($t['rol'] === 'admin') jsonResponse(['error' => 'No se puede eliminar al administrador'], 403);

$db->prepare("DELETE FROM Trabajadores WHERE id = ?")->execute([$id]);

jsonResponse(['success' => true]);
