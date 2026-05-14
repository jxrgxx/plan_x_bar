<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body               = getBody();
$pedido_producto_id = (int)($body['pedido_producto_id'] ?? 0);
$cantidad           = (int)($body['cantidad'] ?? -1);

if (!$pedido_producto_id || $cantidad < 0) jsonResponse(['error' => 'Datos inválidos'], 400);

$db = getDB();

// Obtener pedido_id
$stmt = $db->prepare("SELECT pedido_id FROM PedidoProductos WHERE id = ?");
$stmt->execute([$pedido_producto_id]);
$row = $stmt->fetch();
if (!$row) jsonResponse(['error' => 'Línea no encontrada'], 404);

$pedido_id = $row['pedido_id'];

// Verificar que el pedido está activo (no cobrado)
$stmt = $db->prepare("SELECT id FROM Pedidos WHERE id = ? AND estado IN ('abierto','en_cocina','listo')");
$stmt->execute([$pedido_id]);
if (!$stmt->fetch()) jsonResponse(['error' => 'El pedido ya está cerrado'], 409);

if ($cantidad === 0) {
    $db->prepare("DELETE FROM PedidoProductos WHERE id = ?")->execute([$pedido_producto_id]);
} else {
    $db->prepare("UPDATE PedidoProductos SET cantidad = ? WHERE id = ?")->execute([$cantidad, $pedido_producto_id]);
}

// Recalcular total (excluye líneas canceladas)
$stmt = $db->prepare("SELECT COALESCE(SUM(cantidad * precio_unitario), 0) FROM PedidoProductos WHERE pedido_id = ? AND estado != 'cancelado'");
$stmt->execute([$pedido_id]);
$total = (float)$stmt->fetchColumn();
$db->prepare("UPDATE Pedidos SET subtotal = ?, total = ? WHERE id = ?")->execute([$total, $total, $pedido_id]);

jsonResponse(['success' => true]);
