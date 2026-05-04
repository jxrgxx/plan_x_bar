<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['error' => 'Método no permitido'], 405);
}

$body         = getBody();
$pedido_id    = intval($body['pedido_id'] ?? 0);
$metodo_pago  = $body['metodo_pago'] ?? '';

$permitidos = ['efectivo', 'tarjeta', 'otro'];
if (!$pedido_id || !in_array($metodo_pago, $permitidos)) {
    jsonResponse(['error' => 'Datos inválidos'], 400);
}

$db = getDB();

$stmt = $db->prepare("SELECT mesa_id FROM Pedidos WHERE id = ? AND estado != 'pagado'");
$stmt->execute([$pedido_id]);
$pedido = $stmt->fetch();
if (!$pedido) {
    jsonResponse(['error' => 'Pedido no encontrado o ya cerrado'], 404);
}

$stmt = $db->prepare("UPDATE Pedidos SET estado = 'pagado', metodo_pago = ?, fecha_cierre = NOW() WHERE id = ?");
$stmt->execute([$metodo_pago, $pedido_id]);

// Liberar mesa
$db->prepare("UPDATE Mesas SET estado = 'libre' WHERE id = ?")->execute([$pedido['mesa_id']]);

jsonResponse(['success' => true]);
