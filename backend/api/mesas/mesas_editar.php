<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body      = getBody();
$id        = (int)($body['id'] ?? 0);
$codigo    = trim($body['codigo'] ?? '');
$capacidad = (int)($body['capacidad'] ?? 0);
$posX      = (float)($body['posX'] ?? 0);
$posY      = (float)($body['posY'] ?? 0);
$estado    = trim($body['estado'] ?? 'libre');

$estadosValidos = ['libre', 'ocupada', 'reservada'];
if (!$id || !$codigo || $capacidad < 1) jsonResponse(['error' => 'Datos inválidos'], 400);
if (!in_array($estado, $estadosValidos)) jsonResponse(['error' => 'Estado no válido'], 400);

$db = getDB();

$stmt = $db->prepare("SELECT id FROM Mesas WHERE codigo = ? AND id != ?");
$stmt->execute([$codigo, $id]);
if ($stmt->fetch()) jsonResponse(['error' => 'Ya existe otra mesa con ese código'], 409);

$stmt = $db->prepare("UPDATE Mesas SET codigo = ?, capacidad = ?, posX = ?, posY = ?, estado = ? WHERE id = ?");
$stmt->execute([$codigo, $capacidad, $posX, $posY, $estado, $id]);

jsonResponse(['success' => true]);
