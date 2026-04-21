<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body = getBody();
$id   = (int)($body['id'] ?? 0);

if (!$id) jsonResponse(['error' => 'ID requerido'], 400);

$db = getDB();

$stmt = $db->prepare("SELECT id FROM Productos WHERE id = ?");
$stmt->execute([$id]);
if (!$stmt->fetch()) jsonResponse(['error' => 'Producto no encontrado'], 404);

$db->prepare("DELETE FROM Productos WHERE id = ?")->execute([$id]);

jsonResponse(['success' => true]);
