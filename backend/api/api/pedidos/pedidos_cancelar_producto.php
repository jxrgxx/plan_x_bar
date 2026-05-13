<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body               = getBody();
$pedido_producto_id = (int)($body['pedido_producto_id'] ?? 0);

if (!$pedido_producto_id) jsonResponse(['error' => 'pedido_producto_id requerido'], 400);

try {
    $db = getDB();
    $db->beginTransaction();

    // Obtener la línea y su pedido_id
    $stmt = $db->prepare("SELECT pedido_id, estado FROM PedidoProductos WHERE id = ?");
    $stmt->execute([$pedido_producto_id]);
    $linea = $stmt->fetch();
    if (!$linea) {
        $db->rollBack();
        jsonResponse(['error' => 'Línea no encontrada'], 404);
    }

    // Verificar que no esté ya cancelada
    if ($linea['estado'] === 'cancelado') {
        $db->rollBack();
        jsonResponse(['error' => 'El producto ya estaba cancelado'], 409);
    }

    $pedido_id = $linea['pedido_id'];

    // Verificar que el pedido sigue activo
    $stmt = $db->prepare("SELECT id FROM Pedidos WHERE id = ? AND estado IN ('abierto','en_cocina','listo')");
    $stmt->execute([$pedido_id]);
    if (!$stmt->fetch()) {
        $db->rollBack();
        jsonResponse(['error' => 'El pedido ya está cerrado'], 409);
    }

    // Cancelar la línea
    $db->prepare("UPDATE PedidoProductos SET estado = 'cancelado' WHERE id = ?")
       ->execute([$pedido_producto_id]);

    // Recalcular total excluyendo líneas canceladas
    $stmt = $db->prepare("SELECT COALESCE(SUM(cantidad * precio_unitario), 0) FROM PedidoProductos WHERE pedido_id = ? AND estado != 'cancelado'");
    $stmt->execute([$pedido_id]);
    $total = (float)$stmt->fetchColumn();
    $db->prepare("UPDATE Pedidos SET subtotal = ?, total = ? WHERE id = ?")
       ->execute([$total, $total, $pedido_id]);

    $db->commit();
    jsonResponse(['success' => true]);
} catch (Exception $e) {
    $db->rollBack();
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
