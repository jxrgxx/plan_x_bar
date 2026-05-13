<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body           = getBody();
$restaurante_id = (int)($body['restaurante_id'] ?? 0);
$precio         = (float)($body['precio'] ?? 10.00);
$activo         = isset($body['activo']) ? (int)(bool)$body['activo'] : 1;
$lineas         = $body['lineas'] ?? [];   // array de {producto_id, curso, cantidad}

if (!$restaurante_id) jsonResponse(['error' => 'restaurante_id requerido'], 400);

$cursos_validos = ['bebida', 'primero', 'segundo', 'postre'];

try {
    $db = getDB();

    // Asegurar que existe la tabla MenuDiaLineas
    $db->exec("
        CREATE TABLE IF NOT EXISTS MenuDiaLineas (
            id         INT AUTO_INCREMENT PRIMARY KEY,
            menu_dia_id INT NOT NULL,
            producto_id INT NOT NULL,
            curso      ENUM('bebida','primero','segundo','postre') NOT NULL,
            cantidad   INT NOT NULL DEFAULT 0,
            FOREIGN KEY (menu_dia_id) REFERENCES MenuDia(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    ");

    // Upsert cabecera MenuDia
    $stmt = $db->prepare("SELECT id FROM MenuDia WHERE restaurante_id = ? LIMIT 1");
    $stmt->execute([$restaurante_id]);
    $existing = $stmt->fetch();

    if ($existing) {
        $menu_id = (int)$existing['id'];
        $stmt = $db->prepare("UPDATE MenuDia SET precio=?, activo=? WHERE id=?");
        $stmt->execute([$precio, $activo, $menu_id]);
    } else {
        $stmt = $db->prepare("INSERT INTO MenuDia (restaurante_id, precio, activo) VALUES (?, ?, ?)");
        $stmt->execute([$restaurante_id, $precio, $activo]);
        $menu_id = (int)$db->lastInsertId();
    }

    // Reemplazar lineas: borrar las actuales e insertar las nuevas
    $stmt = $db->prepare("DELETE FROM MenuDiaLineas WHERE menu_dia_id = ?");
    $stmt->execute([$menu_id]);

    $stmt = $db->prepare("
        INSERT INTO MenuDiaLineas (menu_dia_id, producto_id, curso, cantidad)
        VALUES (?, ?, ?, ?)
    ");
    foreach ($lineas as $linea) {
        $producto_id = (int)($linea['producto_id'] ?? 0);
        $curso       = $linea['curso'] ?? '';
        $cantidad    = max(0, (int)($linea['cantidad'] ?? 0));
        if (!$producto_id || !in_array($curso, $cursos_validos)) continue;
        $stmt->execute([$menu_id, $producto_id, $curso, $cantidad]);
    }

    jsonResponse(['success' => true, 'id' => $menu_id]);

} catch (Exception $e) {
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
