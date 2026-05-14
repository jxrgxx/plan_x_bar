<?php
require_once '../config/db.php';

$restaurante_id = (int)($_GET['restaurante_id'] ?? 0);
if (!$restaurante_id) jsonResponse(['error' => 'restaurante_id requerido'], 400);

try {
    $db = getDB();

    // Obtener cabecera del menú
    $stmt = $db->prepare("
        SELECT id, precio, activo
        FROM MenuDia
        WHERE restaurante_id = ? AND activo = 1
        LIMIT 1
    ");
    $stmt->execute([$restaurante_id]);
    $menu = $stmt->fetch();

    if (!$menu) {
        jsonResponse(['menu' => null]);
    }

    $menu_id = (int)$menu['id'];

    // Obtener líneas con datos del producto
    $stmt = $db->prepare("
        SELECT l.id, l.producto_id, l.curso, l.cantidad,
               p.nombre, p.precio
        FROM MenuDiaLineas l
        JOIN Productos p ON p.id = l.producto_id
        WHERE l.menu_dia_id = ?
        ORDER BY FIELD(l.curso,'bebida','primero','segundo','postre'), l.id
    ");
    $stmt->execute([$menu_id]);
    $lineas = $stmt->fetchAll();

    jsonResponse([
        'menu' => [
            'id'     => $menu_id,
            'precio' => (float)$menu['precio'],
            'activo' => (bool)$menu['activo'],
            'lineas' => array_map(fn($l) => [
                'id'          => (int)$l['id'],
                'producto_id' => (int)$l['producto_id'],
                'nombre'      => $l['nombre'],
                'precio'      => (float)$l['precio'],
                'curso'       => $l['curso'],
                'cantidad'    => (int)$l['cantidad'],
            ], $lineas)
        ]
    ]);

} catch (Exception $e) {
    jsonResponse(['error' => $e->getMessage()], 500);
}
