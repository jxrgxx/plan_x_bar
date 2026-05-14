<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['success' => false, 'error' => 'Método no permitido'], 405);

$body      = getBody();
$pedido_id = (int)($body['pedido_id'] ?? 0);
if (!$pedido_id) jsonResponse(['success' => false, 'error' => 'pedido_id requerido'], 400);

try {
    $db = getDB();
    $db->beginTransaction();

    $db->prepare("UPDATE Pedidos SET estado = 'listo' WHERE id = ? AND estado = 'en_cocina'")
       ->execute([$pedido_id]);

    // Solo marcar como preparado los productos de cocina; las bebidas las gestiona el camarero
    $db->prepare("
        UPDATE PedidoProductos pp
        JOIN Productos pr ON pr.id = pp.producto_id
        SET pp.estado = 'preparado'
        WHERE pp.pedido_id = ? AND pp.estado != 'cancelado' AND pr.categoria != 'bebida'
    ")->execute([$pedido_id]);

    $db->commit();
    jsonResponse(['success' => true]);
} catch (Exception $e) {
    $db->rollBack();
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
