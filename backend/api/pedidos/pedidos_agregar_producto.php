<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['error' => 'Método no permitido'], 405);
}

$body          = getBody();
$pedido_id     = intval($body['pedido_id'] ?? 0);
$producto_id   = intval($body['producto_id'] ?? 0);
$cantidad      = intval($body['cantidad'] ?? 1);
$observaciones = trim($body['observaciones'] ?? '');

if (!$pedido_id || !$producto_id || $cantidad < 1) {
    jsonResponse(['error' => 'Datos inválidos'], 400);
}

$db = getDB();

// Obtener precio actual del producto
$stmt = $db->prepare("SELECT precio FROM Productos WHERE id = ? AND disponible = TRUE");
$stmt->execute([$producto_id]);
$producto = $stmt->fetch();
if (!$producto) {
    jsonResponse(['error' => 'Producto no disponible'], 404);
}

$precio = $producto['precio'];

// Solo fusionar con líneas que aún NO se han enviado a cocina (estado = '').
// Si la línea ya está en preparacion/preparado, se crea una línea nueva independiente.
$stmt = $db->prepare("SELECT id, cantidad FROM PedidoProductos WHERE pedido_id = ? AND producto_id = ? AND (observaciones IS NULL OR observaciones = '') AND estado = ''");
$stmt->execute([$pedido_id, $producto_id]);
$lineaExistente = $stmt->fetch();

if ($lineaExistente && empty($observaciones)) {
    $nuevaCantidad = $lineaExistente['cantidad'] + $cantidad;
    $db->prepare("UPDATE PedidoProductos SET cantidad = ? WHERE id = ?")->execute([$nuevaCantidad, $lineaExistente['id']]);
} else {
    $db->prepare("INSERT INTO PedidoProductos (pedido_id, producto_id, cantidad, precio_unitario, observaciones, estado) VALUES (?, ?, ?, ?, ?, '')")
       ->execute([$pedido_id, $producto_id, $cantidad, $precio, $observaciones ?: null]);
}

// Recalcular total del pedido (excluye líneas canceladas)
$stmt = $db->prepare("UPDATE Pedidos SET subtotal = (SELECT COALESCE(SUM(cantidad * precio_unitario), 0) FROM PedidoProductos WHERE pedido_id = ? AND estado != 'cancelado'), total = (SELECT COALESCE(SUM(cantidad * precio_unitario), 0) FROM PedidoProductos WHERE pedido_id = ? AND estado != 'cancelado') WHERE id = ?");
$stmt->execute([$pedido_id, $pedido_id, $pedido_id]);

// Si es plato de menú del día, descontar stock de MenuDiaLineas
if (!empty($observaciones) && strpos($observaciones, 'Menú del día #') === 0) {
    $stmt = $db->prepare("SELECT restaurante_id FROM Pedidos WHERE id = ?");
    $stmt->execute([$pedido_id]);
    $ped = $stmt->fetch();
    if ($ped) {
        $stmt = $db->prepare("
            SELECT mdl.id FROM MenuDiaLineas mdl
            JOIN MenuDia md ON md.id = mdl.menu_dia_id
            WHERE md.restaurante_id = ? AND mdl.producto_id = ? AND md.activo = 1 AND mdl.cantidad > 0
            LIMIT 1
        ");
        $stmt->execute([$ped['restaurante_id'], $producto_id]);
        $linea = $stmt->fetch();
        if ($linea) {
            $db->prepare("UPDATE MenuDiaLineas SET cantidad = cantidad - ? WHERE id = ? AND cantidad >= ?")
               ->execute([$cantidad, $linea['id'], $cantidad]);
        }
    }
}

jsonResponse(['success' => true, 'precio_unitario' => $precio], 201);
