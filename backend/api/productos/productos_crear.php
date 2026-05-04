<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body           = getBody();
$restaurante_id = (int)($body['restaurante_id'] ?? 1);
$nombre         = trim($body['nombre'] ?? '');
$categoria      = trim($body['categoria'] ?? '');
$descripcion    = trim($body['descripcion'] ?? '');
$precio         = (float)($body['precio'] ?? 0);
$disponible     = isset($body['disponible']) ? (bool)$body['disponible'] : true;

$categorias = ['entrante', 'principal', 'postre', 'bebida'];
if (!$nombre || !in_array($categoria, $categorias) || $precio <= 0) {
    jsonResponse(['error' => 'Nombre, categoría válida y precio son obligatorios'], 400);
}

$db = getDB();
$stmt = $db->prepare("INSERT INTO Productos (restaurante_id, nombre, categoria, descripcion, precio, disponible) VALUES (?, ?, ?, ?, ?, ?)");
$stmt->execute([$restaurante_id, $nombre, $categoria, $descripcion, $precio, $disponible]);

jsonResponse(['success' => true, 'id' => (int)$db->lastInsertId()], 201);
