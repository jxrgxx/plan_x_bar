<?php
require_once 'config/db.php';
try {
    $db = getDB();
    // Comprobar si la columna ya existe
    $stmt = $db->query("SHOW COLUMNS FROM Trabajadores LIKE 'pin'");
    if ($stmt->rowCount() > 0) {
        echo json_encode(['ok' => true, 'msg' => 'La columna pin ya existe']);
    } else {
        $db->exec("ALTER TABLE Trabajadores ADD COLUMN pin VARCHAR(255) NULL DEFAULT NULL AFTER email");
        echo json_encode(['ok' => true, 'msg' => 'Columna pin añadida correctamente']);
    }
} catch (Exception $e) {
    echo json_encode(['ok' => false, 'msg' => $e->getMessage()]);
}
