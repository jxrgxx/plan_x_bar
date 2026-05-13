<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['error' => 'Método no permitido'], 405);
}

$body           = getBody();
$restaurante_id = intval($body['restaurante_id'] ?? 1);
$mesa_id        = intval($body['mesa_id'] ?? 0);
$trabajador_id  = intval($body['trabajador_id'] ?? 0) ?: null;
$reserva_id     = intval($body['reserva_id'] ?? 0) ?: null;

if (!$mesa_id) {
    jsonResponse(['error' => 'mesa_id requerido'], 400);
}

$db = getDB();

// Verificar que no haya pedido abierto en esa mesa
$stmt = $db->prepare("SELECT id FROM Pedidos WHERE mesa_id = ? AND estado = 'abierto'");
$stmt->execute([$mesa_id]);
if ($stmt->fetch()) {
    jsonResponse(['error' => 'La mesa ya tiene un pedido abierto'], 409);
}

$stmt = $db->prepare("INSERT INTO Pedidos (restaurante_id, mesa_id, trabajador_id, reserva_id) VALUES (?, ?, ?, ?)");
$stmt->execute([$restaurante_id, $mesa_id, $trabajador_id, $reserva_id]);
$pedido_id = $db->lastInsertId();

// Marcar mesa como ocupada
$db->prepare("UPDATE Mesas SET estado = 'ocupada' WHERE id = ?")->execute([$mesa_id]);

jsonResponse(['success' => true, 'pedido_id' => $pedido_id], 201);
