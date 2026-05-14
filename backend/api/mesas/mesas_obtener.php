<?php
require_once '../config/db.php';

$restaurante_id = intval($_GET['restaurante_id'] ?? 1);

$db = getDB();

// Sincronizar estado de mesas con pedidos activos antes de devolver
// Esto garantiza consistencia aunque pedidos_crear no haya actualizado el estado
$db->prepare("
    UPDATE Mesas m
    SET m.estado = 'ocupada'
    WHERE m.restaurante_id = ?
      AND m.estado != 'ocupada'
      AND EXISTS (
          SELECT 1 FROM Pedidos p
          WHERE p.mesa_id = m.id
            AND p.estado IN ('abierto', 'en_cocina', 'listo')
      )
")->execute([$restaurante_id]);

$db->prepare("
    UPDATE Mesas m
    SET m.estado = 'libre'
    WHERE m.restaurante_id = ?
      AND m.estado = 'ocupada'
      AND NOT EXISTS (
          SELECT 1 FROM Pedidos p
          WHERE p.mesa_id = m.id
            AND p.estado IN ('abierto', 'en_cocina', 'listo')
      )
")->execute([$restaurante_id]);

$stmt = $db->prepare("SELECT id, codigo, capacidad, estado, posX, posY, zona FROM Mesas WHERE restaurante_id = ? ORDER BY codigo");
$stmt->execute([$restaurante_id]);
$mesas = $stmt->fetchAll();

foreach ($mesas as &$m) {
    $m['id']        = (int) $m['id'];
    $m['capacidad'] = (int) $m['capacidad'];
    $m['posX']      = (float) $m['posX'];
    $m['posY']      = (float) $m['posY'];
    $m['zona']      = $m['zona'] ?? 'piso1';
}

jsonResponse(['mesas' => $mesas]);
