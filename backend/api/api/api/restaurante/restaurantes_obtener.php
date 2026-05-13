<?php
require_once '../config/db.php';

try {
    $db   = getDB();
    $stmt = $db->query("SELECT id, nombre, direccion, telefono FROM Restaurante ORDER BY nombre");
    $restaurantes = $stmt->fetchAll();
    foreach ($restaurantes as &$r) $r['id'] = (int)$r['id'];
    jsonResponse(['restaurantes' => $restaurantes]);
} catch (Exception $e) {
    jsonResponse(['error' => $e->getMessage()], 500);
}
