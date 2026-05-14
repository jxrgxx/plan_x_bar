<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body        = getBody();
$id          = (int)($body['id'] ?? 0);
$nombre      = trim($body['nombre'] ?? '');
$categoria   = trim($body['categoria'] ?? '');
$descripcion = trim($body['descripcion'] ?? '');
$precio      = (float)($body['precio'] ?? 0);
$disponible  = isset($body['disponible']) ? (bool)$body['disponible'] : true;

$categorias = ['entrante', 'principal', 'postre', 'bebida'];
if (!$id || !$nombre || !in_array($categoria, $categorias) || $precio <= 0) {
    jsonResponse(['error' => 'Datos inválidos'], 400);
}

$db = getDB();
$stmt = $db->prepare("UPDATE Productos SET nombre = ?, categoria = ?, descripcion = ?, precio = ?, disponible = ? WHERE id = ?");
$stmt->execute([$nombre, $categoria, $descripcion, $precio, $disponible, $id]);

jsonResponse(['success' => true]);
