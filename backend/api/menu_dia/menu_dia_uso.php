<?php
require_once '../config/db.php';

$restaurante_id = (int)($_GET['restaurante_id'] ?? 0);
if (!$restaurante_id) {
    jsonResponse(['uso' => []]);
}

try {
    $db = getDB();

    // Obtener el menú activo del restaurante
    $stmt = $db->prepare("SELECT id FROM MenuDia WHERE restaurante_id = ? AND activo = 1 LIMIT 1");
    $stmt->execute([$restaurante_id]);
    $menu = $stmt->fetch();

    if (!$menu) {
        jsonResponse(['uso' => []]);
    }

    $menu_id = (int)$menu['id'];

    // Solo productos con límite de cantidad
    $stmt = $db->prepare("SELECT producto_id FROM MenuDiaLineas WHERE menu_dia_id = ? AND cantidad > 0");
    $stmt->execute([$menu_id]);
    $producto_ids = $stmt->fetchAll(PDO::FETCH_COLUMN);

    if (empty($producto_ids)) {
        jsonResponse(['uso' => []]);
    }

    $placeholders = implode(',', array_fill(0, count($producto_ids), '?'));
    $params = array_merge([$restaurante_id], $producto_ids);

    // Sumar unidades ya usadas en pedidos activos (abierto o en cocina), sin canceladas
    $stmt = $db->prepare("
        SELECT pp.producto_id, SUM(pp.cantidad) AS usado
        FROM PedidoProductos pp
        JOIN Pedidos p ON p.id = pp.pedido_id
        WHERE p.restaurante_id = ?
          AND p.estado IN ('abierto', 'en_cocina', 'listo', 'cerrado')
          AND pp.estado != 'cancelado'
          AND pp.producto_id IN ($placeholders)
          AND pp.observaciones LIKE 'Menú del día #%'
        GROUP BY pp.producto_id
    ");
    $stmt->execute($params);
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    jsonResponse(['uso' => array_map(fn($r) => [
        'producto_id' => (int)$r['producto_id'],
        'usado'       => (int)$r['usado'],
    ], $rows)]);

} catch (Exception $e) {
    jsonResponse(['uso' => [], 'error' => $e->getMessage()], 500);
}
