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

// Verificar que el pedido no esté ya cobrado/cancelado
$stmt = $db->prepare("SELECT id FROM Pedidos WHERE id = ? AND estado IN ('abierto', 'en_cocina', 'listo')");
$stmt->execute([$pedido_id]);
if (!$stmt->fetch()) jsonResponse(['error' => 'El pedido ya está cerrado'], 409);

// Obtener datos de la línea antes de eliminar (para restaurar stock si es menú del día)
$stmt = $db->prepare("
    SELECT pp.producto_id, pp.cantidad, pp.observaciones, p.restaurante_id
    FROM PedidoProductos pp
    JOIN Pedidos p ON p.id = pp.pedido_id
    WHERE pp.id = ?
");
$stmt->execute([$pedido_producto_id]);
$pp = $stmt->fetch();

// Eliminar línea
$db->prepare("DELETE FROM PedidoProductos WHERE id = ?")->execute([$pedido_producto_id]);

// Recalcular total
$stmt = $db->prepare("SELECT SUM(cantidad * precio_unitario) FROM PedidoProductos WHERE pedido_id = ?");
$stmt->execute([$pedido_id]);
$total = (float)($stmt->fetchColumn() ?? 0);
$db->prepare("UPDATE Pedidos SET subtotal = ?, total = ? WHERE id = ?")->execute([$total, $total, $pedido_id]);

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
