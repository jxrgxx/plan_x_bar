<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body              = getBody();
$pedido_producto_id = (int)($body['pedido_producto_id'] ?? 0);

if (!$pedido_producto_id) jsonResponse(['error' => 'pedido_producto_id requerido'], 400);

$db = getDB();

// Obtener pedido_id antes de eliminar
$stmt = $db->prepare("SELECT pedido_id FROM PedidoProductos WHERE id = ?");
$stmt->execute([$pedido_producto_id]);
$row = $stmt->fetch();
if (!$row) jsonResponse(['error' => 'Línea no encontrada'], 404);

$pedido_id = $row['pedido_id'];

// Verificar que el pedido sigue abierto
$stmt = $db->prepare("SELECT id FROM Pedidos WHERE id = ? AND estado = 'abierto'");
$stmt->execute([$pedido_id]);
if (!$stmt->fetch()) jsonResponse(['error' => 'El pedido ya está cerrado'], 409);

// Eliminar línea
$db->prepare("DELETE FROM PedidoProductos WHERE id = ?")->execute([$pedido_producto_id]);

// Recalcular total
$stmt = $db->prepare("SELECT SUM(cantidad * precio_unitario) FROM PedidoProductos WHERE pedido_id = ?");
$stmt->execute([$pedido_id]);
$total = (float)($stmt->fetchColumn() ?? 0);
$db->prepare("UPDATE Pedidos SET subtotal = ?, total = ? WHERE id = ?")->execute([$total, $total, $pedido_id]);

jsonResponse(['success' => true]);
