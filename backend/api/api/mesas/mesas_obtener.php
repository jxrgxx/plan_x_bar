<?php
require_once '../config/db.php';

$restaurante_id = intval($_GET['restaurante_id'] ?? 1);

$db   = getDB();
$stmt = $db->prepare("SELECT id, codigo, capacidad, estado, posX, posY FROM Mesas WHERE restaurante_id = ? ORDER BY codigo");
$stmt->execute([$restaurante_id]);
$mesas = $stmt->fetchAll();

jsonResponse(['mesas' => $mesas]);
