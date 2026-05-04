<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body      = getBody();
$pedido_id = (int) ($body['pedido_id'] ?? 0);

if (!$pedido_id) jsonResponse(['success' => false, 'error' => 'pedido_id requerido'], 400);

try {
    $db = getDB();
    $db->beginTransaction();

    // Verificar que el pedido existe y no está pagado
    $stmt = $db->prepare("SELECT estado FROM Pedidos WHERE id = ? AND estado IN ('abierto', 'en_cocina', 'listo')");
    $stmt->execute([$pedido_id]);
    $pedido = $stmt->fetch();

    if (!$pedido) {
        $db->rollBack();
        jsonResponse(['success' => false, 'error' => 'Pedido no encontrado o ya cerrado']);
    }

    // Si estaba abierto, pasarlo a en_cocina
    if ($pedido['estado'] === 'abierto') {
        $db->prepare("UPDATE Pedidos SET estado = 'en_cocina' WHERE id = ?")
           ->execute([$pedido_id]);
    }

    // Solo enviar productos que aún no se han mandado a cocina (estado = '')
    $stmt = $db->prepare("UPDATE PedidoProductos SET estado = 'en preparacion' WHERE pedido_id = ? AND estado = ''");
    $stmt->execute([$pedido_id]);

    if ($stmt->rowCount() === 0) {
        $db->rollBack();
        jsonResponse(['success' => false, 'error' => 'No hay productos nuevos para enviar']);
    }

    $db->commit();
    jsonResponse(['success' => true]);
} catch (Exception $e) {
    $db->rollBack();
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
