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

    $pedido['id']             = (int)$pedido['id'];
    $pedido['mesa_id']        = (int)$pedido['mesa_id'];
    $pedido['restaurante_id'] = (int)$pedido['restaurante_id'];
    $pedido['trabajador_id']  = $pedido['trabajador_id'] !== null ? (int)$pedido['trabajador_id'] : null;
    $pedido['reserva_id']     = $pedido['reserva_id']    !== null ? (int)$pedido['reserva_id']    : null;
    $pedido['subtotal']       = (float)($pedido['subtotal'] ?? 0);
    $pedido['total']          = (float)($pedido['total']    ?? 0);

    $stmt = $db->prepare("
        SELECT pp.id, pp.cantidad, pp.precio_unitario, pp.observaciones, pp.fecha_agregado,
               pr.nombre, pr.categoria
        FROM PedidoProductos pp
        JOIN Productos pr ON pr.id = pp.producto_id
        WHERE pp.pedido_id = ?
        ORDER BY pp.fecha_agregado
    ");
    $stmt->execute([$pedido_id]);
    $productos = $stmt->fetchAll();

    foreach ($productos as &$pp) {
        $pp['id']              = (int)$pp['id'];
        $pp['cantidad']        = (int)$pp['cantidad'];
        $pp['precio_unitario'] = (float)$pp['precio_unitario'];
    }

    $pedido['productos'] = $productos;

    jsonResponse(['pedido' => $pedido]);
}

if ($mesa_id) {
    $stmt = $db->prepare("
        SELECT p.id, p.estado, p.fecha_apertura, p.total, p.trabajador_id
        FROM Pedidos p WHERE p.mesa_id = ? AND p.estado = 'abierto' LIMIT 1
    ");
    $stmt->execute([$mesa_id]);
    $pedido = $stmt->fetch();
    if ($pedido) {
        $pedido['id']            = (int)$pedido['id'];
        $pedido['trabajador_id'] = $pedido['trabajador_id'] !== null ? (int)$pedido['trabajador_id'] : null;
        $pedido['total']         = (float)($pedido['total'] ?? 0);
    }
    jsonResponse(['pedido' => $pedido ?: null]);
}

jsonResponse(['error' => 'Se requiere pedido_id o mesa_id'], 400);
