<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body           = getBody();
$restaurante_id = (int)($body['restaurante_id'] ?? 1);
$codigo         = trim($body['codigo'] ?? '');
$capacidad      = (int)($body['capacidad'] ?? 0);
$posX           = (float)($body['posX'] ?? 0);
$posY           = (float)($body['posY'] ?? 0);
$zona_id        = (int)($body['zona_id'] ?? 0);

if (!$codigo || $capacidad < 1 || !$zona_id) {
    jsonResponse(['error' => 'Código, capacidad y zona son obligatorios'], 400);
}

$db = getDB();

// Verificar que la zona pertenece al restaurante
$stmtZona = $db->prepare("SELECT id FROM Zonas WHERE id = ? AND restaurante_id = ? AND activo = 1");
$stmtZona->execute([$zona_id, $restaurante_id]);
if (!$stmtZona->fetch()) jsonResponse(['error' => 'Zona no válida'], 400);

$stmt = $db->prepare("SELECT id FROM Mesas WHERE restaurante_id = ? AND codigo = ?");
$stmt->execute([$restaurante_id, $codigo]);
if ($stmt->fetch()) jsonResponse(['error' => 'Ya existe una mesa con ese código'], 409);

$stmt = $db->prepare("INSERT INTO Mesas (restaurante_id, codigo, capacidad, posX, posY, zona_id) VALUES (?, ?, ?, ?, ?, ?)");
$stmt->execute([$restaurante_id, $codigo, $capacidad, $posX, $posY, $zona_id]);

jsonResponse(['success' => true, 'id' => (int)$db->lastInsertId()], 201);
