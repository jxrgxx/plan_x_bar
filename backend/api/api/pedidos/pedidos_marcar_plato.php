<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body              = getBody();
$pedido_producto_id = (int) ($body['pedido_producto_id'] ?? 0);
$estado            = trim($body['estado'] ?? '');

$estadosValidos = ['en preparacion', 'preparado', 'servido'];
if (!$pedido_producto_id || !in_array($estado, $estadosValidos)) {
    jsonResponse(['success' => false, 'error' => 'Datos inválidos'], 400);
}

try {
    $db   = getDB();
    $stmt = $db->prepare("UPDATE PedidoProductos SET estado = ? WHERE id = ?");
    $stmt->execute([$estado, $pedido_producto_id]);
    jsonResponse(['success' => true]);
} catch (Exception $e) {
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
