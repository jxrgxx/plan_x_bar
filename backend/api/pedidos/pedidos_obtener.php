<?php
require_once '../config/db.php';

$pedido_id = intval($_GET['pedido_id'] ?? 0);
$mesa_id   = intval($_GET['mesa_id'] ?? 0);

$db = getDB();

if ($pedido_id) {
    $stmt = $db->prepare("SELECT p.*, m.codigo AS mesa_codigo FROM Pedidos p JOIN Mesas m ON m.id = p.mesa_id WHERE p.id = ?");
    $stmt->execute([$pedido_id]);
    $pedido = $stmt->fetch();

    if (!$pedido) {
        jsonResponse(['error' => 'Pedido no encontrado'], 404);
    }

    $stmt = $db->prepare("
        SELECT pp.id, pp.cantidad, pp.precio_unitario, pp.observaciones, pp.fecha_agregado,
               pr.nombre, pr.categoria
        FROM PedidoProductos pp
        JOIN Productos pr ON pr.id = pp.producto_id
        WHERE pp.pedido_id = ?
        ORDER BY pp.fecha_agregado
    ");
    $stmt->execute([$pedido_id]);
    $pedido['productos'] = $stmt->fetchAll();

    jsonResponse(['pedido' => $pedido]);
}

if ($mesa_id) {
    $stmt = $db->prepare("
        SELECT p.id, p.estado, p.fecha_apertura, p.total, p.trabajador_id
        FROM Pedidos p WHERE p.mesa_id = ? AND p.estado = 'abierto' LIMIT 1
    ");
    $stmt->execute([$mesa_id]);
    $pedido = $stmt->fetch();
    jsonResponse(['pedido' => $pedido ?: null]);
}

jsonResponse(['error' => 'Se requiere pedido_id o mesa_id'], 400);
