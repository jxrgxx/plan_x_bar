<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['error' => 'Método no permitido'], 405);
}

$body         = getBody();
$pedido_id    = intval($body['pedido_id'] ?? 0);
$metodo_pago  = $body['metodo_pago'] ?? '';
// total_final: si se envía, se usa este importe (permite aplicar descuentos del lado cliente)
$total_final  = isset($body['total_final']) ? floatval($body['total_final']) : null;

$permitidos = ['efectivo', 'tarjeta', 'otro'];
if (!$pedido_id || !in_array($metodo_pago, $permitidos)) {
    jsonResponse(['error' => 'Datos inválidos'], 400);
}

$db = getDB();

$stmt = $db->prepare("SELECT mesa_id, total FROM Pedidos WHERE id = ? AND estado != 'pagado'");
$stmt->execute([$pedido_id]);
$pedido = $stmt->fetch();
if (!$pedido) {
    jsonResponse(['error' => 'Pedido no encontrado o ya cerrado'], 404);
}

// Usar el total enviado por el cliente si está disponible (incluye descuentos),
// de lo contrario usar el total almacenado en BD.
$total_cobrar = ($total_final !== null) ? $total_final : floatval($pedido['total'] ?? 0);

$stmt = $db->prepare("UPDATE Pedidos SET estado = 'pagado', metodo_pago = ?, total = ?, fecha_cierre = NOW() WHERE id = ?");
$stmt->execute([$metodo_pago, $total_cobrar, $pedido_id]);

$db->prepare("UPDATE Mesas SET estado = 'libre' WHERE id = ?")->execute([$pedido['mesa_id']]);

jsonResponse(['success' => true]);
