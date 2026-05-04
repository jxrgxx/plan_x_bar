<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['error' => 'Método no permitido'], 405);
}

$body          = getBody();
$pedido_id     = intval($body['pedido_id'] ?? 0);
$producto_id   = intval($body['producto_id'] ?? 0);
$cantidad      = intval($body['cantidad'] ?? 1);
$observaciones = trim($body['observaciones'] ?? '');

if (!$pedido_id || !$producto_id || $cantidad < 1) {
    jsonResponse(['error' => 'Datos inválidos'], 400);
}

$db = getDB();

// Obtener precio actual del producto
$stmt = $db->prepare("SELECT precio FROM Productos WHERE id = ? AND disponible = TRUE");
$stmt->execute([$producto_id]);
$producto = $stmt->fetch();
if (!$producto) {
    jsonResponse(['error' => 'Producto no disponible'], 404);
}

$precio = $producto['precio'];

// Si ya existe una línea para este producto (sin observaciones especiales), sumar cantidad
$stmt = $db->prepare("SELECT id, cantidad FROM PedidoProductos WHERE pedido_id = ? AND producto_id = ? AND (observaciones IS NULL OR observaciones = '')");
$stmt->execute([$pedido_id, $producto_id]);
$lineaExistente = $stmt->fetch();

if ($lineaExistente && empty($observaciones)) {
    $nuevaCantidad = $lineaExistente['cantidad'] + $cantidad;
    $db->prepare("UPDATE PedidoProductos SET cantidad = ? WHERE id = ?")->execute([$nuevaCantidad, $lineaExistente['id']]);
} else {
    $db->prepare("INSERT INTO PedidoProductos (pedido_id, producto_id, cantidad, precio_unitario, observaciones, estado) VALUES (?, ?, ?, ?, ?, '')")
       ->execute([$pedido_id, $producto_id, $cantidad, $precio, $observaciones ?: null]);
}

// Recalcular total del pedido
$stmt = $db->prepare("UPDATE Pedidos SET subtotal = (SELECT SUM(cantidad * precio_unitario) FROM PedidoProductos WHERE pedido_id = ?), total = (SELECT SUM(cantidad * precio_unitario) FROM PedidoProductos WHERE pedido_id = ?) WHERE id = ?");
$stmt->execute([$pedido_id, $pedido_id, $pedido_id]);

jsonResponse(['success' => true, 'precio_unitario' => $precio], 201);
