<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body               = getBody();
$pedido_producto_id = (int)($body['pedido_producto_id'] ?? 0);

if (!$pedido_producto_id) jsonResponse(['error' => 'pedido_producto_id requerido'], 400);

try {
    $db   = getDB();
    $stmt = $db->prepare("UPDATE PedidoProductos SET estado = 'cancelado' WHERE id = ?");
    $stmt->execute([$pedido_producto_id]);
    jsonResponse(['success' => true]);
} catch (Exception $e) {
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
