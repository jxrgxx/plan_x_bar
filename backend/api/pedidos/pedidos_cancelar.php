<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body      = getBody();
$pedido_id = (int)($body['pedido_id'] ?? 0);

if (!$pedido_id) jsonResponse(['error' => 'pedido_id requerido'], 400);

$db = getDB();

// Buscar el pedido (cualquier estado excepto pagado)
$stmt = $db->prepare("SELECT mesa_id FROM Pedidos WHERE id = ? AND estado != 'pagado'");
$stmt->execute([$pedido_id]);
$pedido = $stmt->fetch();
if (!$pedido) jsonResponse(['error' => 'Pedido no encontrado o ya cerrado'], 404);

// Solo cancelar si no quedan productos activos (los cancelados no cuentan)
$stmt = $db->prepare("SELECT COUNT(*) FROM PedidoProductos WHERE pedido_id = ? AND estado != 'cancelado'");
$stmt->execute([$pedido_id]);
if ($stmt->fetchColumn() > 0) jsonResponse(['error' => 'El pedido tiene productos activos'], 409);

// Eliminar productos (incluidos cancelados) y el pedido, luego liberar mesa
$db->prepare("DELETE FROM PedidoProductos WHERE pedido_id = ?")->execute([$pedido_id]);
$db->prepare("DELETE FROM Pedidos WHERE id = ?")->execute([$pedido_id]);
$db->prepare("UPDATE Mesas SET estado = 'libre' WHERE id = ?")->execute([$pedido['mesa_id']]);

jsonResponse(['success' => true]);
