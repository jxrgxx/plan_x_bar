<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body      = getBody();
$pedido_id = (int) ($body['pedido_id'] ?? 0);

if (!$pedido_id) jsonResponse(['success' => false, 'error' => 'pedido_id requerido'], 400);

try {
    $db = getDB();
    $db->beginTransaction();

    $stmt = $db->prepare("UPDATE Pedidos SET estado = 'en_cocina' WHERE id = ? AND estado = 'abierto'");
    $stmt->execute([$pedido_id]);

    if ($stmt->rowCount() === 0) {
        $db->rollBack();
        jsonResponse(['success' => false, 'error' => 'Pedido no encontrado o ya enviado']);
    }

    $db->prepare("UPDATE PedidoProductos SET estado = 'en preparacion' WHERE pedido_id = ?")
       ->execute([$pedido_id]);

    $db->commit();
    jsonResponse(['success' => true]);
} catch (Exception $e) {
    $db->rollBack();
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
