<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body               = getBody();
$pedido_producto_id = (int)($body['pedido_producto_id'] ?? 0);

if (!$pedido_producto_id) jsonResponse(['error' => 'pedido_producto_id requerido'], 400);

try {
    $db = getDB();

    // Obtener datos de la línea antes de cancelar
    $stmt = $db->prepare("
        SELECT pp.producto_id, pp.cantidad, pp.observaciones, p.restaurante_id
        FROM PedidoProductos pp
        JOIN Pedidos p ON p.id = pp.pedido_id
        WHERE pp.id = ? AND pp.estado != 'cancelado'
    ");
    $stmt->execute([$pedido_producto_id]);
    $pp = $stmt->fetch();

    $stmt = $db->prepare("UPDATE PedidoProductos SET estado = 'cancelado' WHERE id = ?");
    $stmt->execute([$pedido_producto_id]);

    // Si era plato de menú del día, restaurar stock
    if ($pp && !empty($pp['observaciones']) && strpos($pp['observaciones'], 'Menú del día #') === 0) {
        $stmt = $db->prepare("
            SELECT mdl.id FROM MenuDiaLineas mdl
            JOIN MenuDia md ON md.id = mdl.menu_dia_id
            WHERE md.restaurante_id = ? AND mdl.producto_id = ? AND md.activo = 1
            LIMIT 1
        ");
        $stmt->execute([$pp['restaurante_id'], $pp['producto_id']]);
        $linea = $stmt->fetch();
        if ($linea) {
            $db->prepare("UPDATE MenuDiaLineas SET cantidad = cantidad + ? WHERE id = ? AND cantidad >= 0")
               ->execute([$pp['cantidad'], $linea['id']]);
        }
    }

    jsonResponse(['success' => true]);
} catch (Exception $e) {
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
