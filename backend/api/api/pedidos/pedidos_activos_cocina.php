<?php
require_once '../config/db.php';

$restaurante_id = (int) ($_GET['restaurante_id'] ?? 0);
if (!$restaurante_id) jsonResponse(['error' => 'restaurante_id requerido'], 400);

try {
    $db = getDB();

    $stmt = $db->prepare("
        SELECT p.id, p.mesa_id, p.trabajador_id, p.fecha_apertura, m.codigo AS mesa_codigo
        FROM Pedidos p
        JOIN Mesas m ON m.id = p.mesa_id
        WHERE p.restaurante_id = ?
          AND (
            p.estado = 'en_cocina'
            OR (
                p.estado IN ('pagado', 'listo')
                AND EXISTS (
                    SELECT 1 FROM PedidoProductos pp2
                    JOIN Productos pr2 ON pr2.id = pp2.producto_id
                    WHERE pp2.pedido_id = p.id
                      AND pp2.estado = 'en preparacion'
                      AND pr2.categoria != 'bebida'
                )
            )
          )
        ORDER BY p.fecha_apertura
    ");
    $stmt->execute([$restaurante_id]);
    $pedidos = $stmt->fetchAll();

    foreach ($pedidos as &$pedido) {
        $pedido['id']           = (int) $pedido['id'];
        $pedido['mesa_id']      = (int) $pedido['mesa_id'];
        $pedido['trabajador_id']= $pedido['trabajador_id'] ? (int) $pedido['trabajador_id'] : null;

        $ps = $db->prepare("
            SELECT pp.id, pp.producto_id, pp.cantidad, pp.precio_unitario, pp.observaciones,
                   pp.fecha_agregado, pp.estado, pr.nombre, pr.categoria
            FROM PedidoProductos pp
            JOIN Productos pr ON pr.id = pp.producto_id
            WHERE pp.pedido_id = ? AND pp.estado IN ('en preparacion','preparado','cancelado') AND pr.categoria != 'bebida'
            ORDER BY pp.fecha_agregado
        ");
        $ps->execute([$pedido['id']]);
        $productos = $ps->fetchAll();
        foreach ($productos as &$prod) {
            $prod['id']       = (int) $prod['id'];
            $prod['cantidad'] = (int) $prod['cantidad'];
            $prod['precio_unitario'] = (float) $prod['precio_unitario'];
        }
        $pedido['productos'] = $productos;
    }

    jsonResponse(['pedidos' => $pedidos]);
} catch (Exception $e) {
    jsonResponse(['error' => $e->getMessage()], 500);
}
