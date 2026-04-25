<?php
require_once '../config/db.php';

$restaurante_id = (int)($_GET['restaurante_id'] ?? 1);

$db   = getDB();
$stmt = $db->prepare("SELECT id, nombre, rol, email, activo FROM Trabajadores WHERE restaurante_id = ? ORDER BY rol, nombre");
$stmt->execute([$restaurante_id]);
$trabajadores = $stmt->fetchAll();

foreach ($trabajadores as &$t) {
    $t['id']             = (int)$t['id'];
    $t['restaurante_id'] = $restaurante_id;
    $t['activo']         = (bool)$t['activo'];
}

jsonResponse(['trabajadores' => $trabajadores]);
