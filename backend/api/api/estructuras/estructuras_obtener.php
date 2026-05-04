<?php
require_once '../config/db.php';

$restaurante_id = (int) ($_GET['restaurante_id'] ?? 0);
if (!$restaurante_id) jsonResponse(['error' => 'restaurante_id requerido'], 400);

try {
    $db   = getDB();
    $stmt = $db->prepare("SELECT id, restaurante_id, nombre, posX, posY, ancho, alto, color FROM Estructuras WHERE restaurante_id = ? ORDER BY id");
    $stmt->execute([$restaurante_id]);
    $rows = $stmt->fetchAll();
    foreach ($rows as &$r) {
        $r['id']            = (int) $r['id'];
        $r['restaurante_id']= (int) $r['restaurante_id'];
        $r['posX']          = (float) $r['posX'];
        $r['posY']          = (float) $r['posY'];
        $r['ancho']         = (float) $r['ancho'];
        $r['alto']          = (float) $r['alto'];
    }
    jsonResponse(['estructuras' => $rows]);
} catch (Exception $e) {
    jsonResponse(['error' => $e->getMessage()], 500);
}
