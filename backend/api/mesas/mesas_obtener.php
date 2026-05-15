<?php
require_once '../config/db.php';

$restaurante_id = intval($_GET['restaurante_id'] ?? 1);

$db = getDB();

// Sincronizar estado de mesas con pedidos activos
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

$stmt = $db->prepare("
    SELECT m.id, m.codigo, m.capacidad, m.estado,
           m.posX, m.posY, m.ancho, m.alto, m.rotacion,
           m.zona_id,
           z.clave AS zona
    FROM Mesas m
    JOIN Zonas z ON z.id = m.zona_id
    WHERE m.restaurante_id = ?
    ORDER BY m.codigo
");
$stmt->execute([$restaurante_id]);
$mesas = $stmt->fetchAll();

foreach ($mesas as &$m) {
    $m['id']        = (int)   $m['id'];
    $m['capacidad'] = (int)   $m['capacidad'];
    $m['posX']      = (float) $m['posX'];
    $m['posY']      = (float) $m['posY'];
    $m['ancho']     = (float) ($m['ancho']    ?? 100);
    $m['alto']      = (float) ($m['alto']     ?? 100);
    $m['rotacion']  = (float) ($m['rotacion'] ?? 0);
    $m['zona_id']   = (int)   $m['zona_id'];
    $m['zona']      = $m['zona'] ?? 'piso1';
}

jsonResponse(['mesas' => $mesas]);
