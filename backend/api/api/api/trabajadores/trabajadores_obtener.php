<?php
require_once '../config/db.php';

$restaurante_id = (int)($_GET['restaurante_id'] ?? 1);

$db   = getDB();
$stmt = $db->prepare("SELECT id, nombre, rol, email, activo, (pin IS NOT NULL) AS tiene_pin FROM Trabajadores WHERE restaurante_id = ? ORDER BY nombre");
$stmt->execute([$restaurante_id]);
$trabajadores = $stmt->fetchAll();

foreach ($trabajadores as &$t) {
    $t['id']             = (int)$t['id'];
    $t['restaurante_id'] = $restaurante_id;
    $t['activo']         = (bool)$t['activo'];
    $t['tiene_pin']      = (bool)$t['tiene_pin'];
}

jsonResponse(['trabajadores' => $trabajadores]);
