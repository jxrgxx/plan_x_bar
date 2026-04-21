<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body           = getBody();
$restaurante_id = (int)($body['restaurante_id'] ?? 1);
$codigo         = trim($body['codigo'] ?? '');
$capacidad      = (int)($body['capacidad'] ?? 0);
$posX           = (float)($body['posX'] ?? 0);
$posY           = (float)($body['posY'] ?? 0);

if (!$codigo || $capacidad < 1) jsonResponse(['error' => 'Código y capacidad son obligatorios'], 400);

$db = getDB();

$stmt = $db->prepare("SELECT id FROM Mesas WHERE restaurante_id = ? AND codigo = ?");
$stmt->execute([$restaurante_id, $codigo]);
if ($stmt->fetch()) jsonResponse(['error' => 'Ya existe una mesa con ese código'], 409);

$stmt = $db->prepare("INSERT INTO Mesas (restaurante_id, codigo, capacidad, posX, posY) VALUES (?, ?, ?, ?, ?)");
$stmt->execute([$restaurante_id, $codigo, $capacidad, $posX, $posY]);

jsonResponse(['success' => true, 'id' => (int)$db->lastInsertId()], 201);
