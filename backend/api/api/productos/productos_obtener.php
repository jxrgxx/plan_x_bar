<?php
require_once '../config/db.php';

$restaurante_id = intval($_GET['restaurante_id'] ?? 1);
$categoria      = $_GET['categoria'] ?? null;

$db  = getDB();
$sql = "SELECT id, nombre, categoria, descripcion, precio, disponible, imagen_url FROM Productos WHERE restaurante_id = ? AND disponible = TRUE";
$params = [$restaurante_id];

if ($categoria) {
    $permitidas = ['entrante', 'principal', 'postre', 'bebida'];
    if (in_array($categoria, $permitidas)) {
        $sql .= " AND categoria = ?";
        $params[] = $categoria;
    }
}

$sql .= " ORDER BY categoria, nombre";
$stmt = $db->prepare($sql);
$stmt->execute($params);
$productos = array_map(function($p) {
    $p['id']         = (int)$p['id'];
    $p['disponible'] = (bool)$p['disponible'];
    $p['precio']     = (float)$p['precio'];
    return $p;
}, $stmt->fetchAll());

jsonResponse(['productos' => $productos]);
