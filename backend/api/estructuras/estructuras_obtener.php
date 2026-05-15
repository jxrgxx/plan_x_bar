<?php
require_once '../config/db.php';

$restaurante_id = (int) ($_GET['restaurante_id'] ?? 0);
if (!$restaurante_id) jsonResponse(['error' => 'restaurante_id requerido'], 400);

try {
    $db   = getDB();
    $stmt = $db->prepare("
        SELECT e.id, e.restaurante_id, e.nombre,
               e.posX, e.posY, e.ancho, e.alto, e.color, e.rotacion,
               e.zona_id,
               z.clave AS zona
        FROM Estructuras e
        JOIN Zonas z ON z.id = e.zona_id
        WHERE e.restaurante_id = ?
        ORDER BY e.id
    ");
    $stmt->execute([$restaurante_id]);
    $rows = $stmt->fetchAll();
    foreach ($rows as &$r) {
        $r['id']             = (int)   $r['id'];
        $r['restaurante_id'] = (int)   $r['restaurante_id'];
        $r['posX']           = (float) $r['posX'];
        $r['posY']           = (float) $r['posY'];
        $r['ancho']          = (float) $r['ancho'];
        $r['alto']           = (float) $r['alto'];
        $r['zona_id']        = (int)   $r['zona_id'];
        $r['zona']           = $r['zona'] ?? 'piso1';
        $r['rotacion']       = (float) ($r['rotacion'] ?? 0);
    }
    jsonResponse(['estructuras' => $rows]);
} catch (Exception $e) {
    jsonResponse(['error' => $e->getMessage()], 500);
}
