<?php
require_once '../config/db.php';

$restaurante_id = intval($_GET['restaurante_id'] ?? 0);
if (!$restaurante_id) jsonResponse(['error' => 'Falta restaurante_id'], 400);

$db   = getDB();
$stmt = $db->prepare(
    "SELECT id, clave, nombre, orden, activo
     FROM Zonas
     WHERE restaurante_id = ?
     ORDER BY orden"
);
$stmt->execute([$restaurante_id]);
$zonas = $stmt->fetchAll();

foreach ($zonas as &$z) {
    $z['id']     = (int)  $z['id'];
    $z['orden']  = (int)  $z['orden'];
    $z['activo'] = (bool) $z['activo'];
}

jsonResponse(['zonas' => $zonas]);
