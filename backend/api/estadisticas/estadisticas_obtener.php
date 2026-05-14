<?php
require_once '../config/db.php';

$restaurante_id = (int)($_GET['restaurante_id'] ?? 0);
$fecha_inicio   = $_GET['fecha_inicio'] ?? date('Y-m-d');
$fecha_fin      = $_GET['fecha_fin']    ?? date('Y-m-d');

if (!$restaurante_id) jsonResponse(['error' => 'restaurante_id requerido'], 400);

try {
    $db = getDB();

    // ── Venta total (pedidos pagados en el rango) ────────────────────────
    $stmt = $db->prepare("
        SELECT
            COALESCE(SUM(total), 0) AS venta_bruta,
            COALESCE(SUM(total), 0) AS venta_neta
        FROM Pedidos
        WHERE restaurante_id = ?
          AND estado = 'pagado'
          AND DATE(fecha_cierre) BETWEEN ? AND ?
    ");
    $stmt->execute([$restaurante_id, $fecha_inicio, $fecha_fin]);
    $ventas = $stmt->fetch();

    // ── Número de pedidos pagados (proxy de PAX, ya que no almacenamos comensales) ──
    $stmt = $db->prepare("
        SELECT COUNT(id) AS total_pedidos
        FROM Pedidos
        WHERE restaurante_id = ?
          AND estado = 'pagado'
          AND DATE(fecha_cierre) BETWEEN ? AND ?
    ");
    $stmt->execute([$restaurante_id, $fecha_inicio, $fecha_fin]);
    $pax = $stmt->fetch();

    // ── Tiempo medio de servicio (minutos) ───────────────────────────────
    $stmt = $db->prepare("
        SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, fecha_apertura, fecha_cierre)), 0) AS tiempo_medio
        FROM Pedidos
        WHERE restaurante_id = ?
          AND estado = 'pagado'
          AND DATE(fecha_cierre) BETWEEN ? AND ?
    ");
    $stmt->execute([$restaurante_id, $fecha_inicio, $fecha_fin]);
    $tiempo = $stmt->fetch();

    // ── Top 3 platos ─────────────────────────────────────────────────────
    $stmt = $db->prepare("
        SELECT pr.nombre, pr.categoria,
               SUM(pp.cantidad)                        AS total_unidades,
               SUM(pp.cantidad * pp.precio_unitario)   AS total_importe
        FROM PedidoProductos pp
        JOIN Productos pr  ON pr.id  = pp.producto_id
        JOIN Pedidos p     ON p.id   = pp.pedido_id
        WHERE p.restaurante_id = ?
          AND p.estado = 'pagado'
          AND pp.estado != 'cancelado'
          AND DATE(p.fecha_cierre) BETWEEN ? AND ?
        GROUP BY pr.id, pr.nombre, pr.categoria
        ORDER BY total_unidades DESC
        LIMIT 3
    ");
    $stmt->execute([$restaurante_id, $fecha_inicio, $fecha_fin]);
    $top_platos = $stmt->fetchAll();
    foreach ($top_platos as &$tp) {
        $tp['total_unidades'] = (int)$tp['total_unidades'];
        $tp['total_importe']  = (float)$tp['total_importe'];
    }

    // ── Ventas por categoría ─────────────────────────────────────────────
    $stmt = $db->prepare("
        SELECT pr.categoria,
               SUM(pp.cantidad)                        AS total_unidades,
               SUM(pp.cantidad * pp.precio_unitario)   AS total_importe
        FROM PedidoProductos pp
        JOIN Productos pr ON pr.id = pp.producto_id
        JOIN Pedidos p    ON p.id  = pp.pedido_id
        WHERE p.restaurante_id = ?
          AND p.estado = 'pagado'
          AND pp.estado != 'cancelado'
          AND DATE(p.fecha_cierre) BETWEEN ? AND ?
        GROUP BY pr.categoria
        ORDER BY total_importe DESC
    ");
    $stmt->execute([$restaurante_id, $fecha_inicio, $fecha_fin]);
    $ventas_categoria = $stmt->fetchAll();
    foreach ($ventas_categoria as &$vc) {
        $vc['total_unidades'] = (int)$vc['total_unidades'];
        $vc['total_importe']  = (float)$vc['total_importe'];
    }

    // ── Ventas por mesero ────────────────────────────────────────────────
    $stmt = $db->prepare("
        SELECT t.nombre,
               COUNT(p.id)          AS total_pedidos,
               SUM(p.total)         AS total_importe
        FROM Pedidos p
        JOIN Trabajadores t ON t.id = p.trabajador_id
        WHERE p.restaurante_id = ?
          AND p.estado = 'pagado'
          AND DATE(p.fecha_cierre) BETWEEN ? AND ?
        GROUP BY t.id, t.nombre
        ORDER BY total_importe DESC
    ");
    $stmt->execute([$restaurante_id, $fecha_inicio, $fecha_fin]);
    $ventas_mesero = $stmt->fetchAll();
    foreach ($ventas_mesero as &$vm) {
        $vm['total_pedidos'] = (int)$vm['total_pedidos'];
        $vm['total_importe'] = (float)$vm['total_importe'];
    }

    jsonResponse([
        'venta_bruta'           => (float)$ventas['venta_bruta'],
        'venta_neta'            => (float)$ventas['venta_neta'],
        'total_descuentos'      => 0.0,
        'total_cortesias'       => 0.0,
        'total_pax'             => (int)$pax['total_pedidos'],
        'tiempo_medio_minutos'  => (int)$tiempo['tiempo_medio'],
        'top_platos'            => $top_platos,
        'ventas_categoria'      => $ventas_categoria,
        'ventas_mesero'         => $ventas_mesero,
    ]);

} catch (Exception $e) {
    jsonResponse(['error' => $e->getMessage()], 500);
}
