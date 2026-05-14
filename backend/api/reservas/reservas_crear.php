<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['success' => false, 'error' => 'Método no permitido'], 405);

$body           = getBody();
$restaurante_id = (int)($body['restaurante_id'] ?? 0);
$nombre         = trim($body['nombre'] ?? '');
$telefono       = trim($body['telefono'] ?? '');
$correo         = trim($body['correo'] ?? '');
$num_personas   = (int)($body['num_personas'] ?? 0);
$fecha          = trim($body['fecha'] ?? '');
$hora           = trim($body['hora'] ?? '');
$notas          = trim($body['notas'] ?? '');

if (!$restaurante_id || !$nombre || !$telefono || !$num_personas || !$fecha || !$hora) {
    jsonResponse(['success' => false, 'error' => 'Faltan campos obligatorios']);
}

try {
    $db = getDB();

    // Generar código único de 6 caracteres
    do {
        $codigo = strtoupper(substr(md5(uniqid(mt_rand(), true)), 0, 6));
        $check  = $db->prepare("SELECT id FROM Reservas WHERE codigo = ?");
        $check->execute([$codigo]);
    } while ($check->fetch());

    $stmt = $db->prepare("
        INSERT INTO Reservas (codigo, restaurante_id, nombre, telefono, correo, num_personas, fecha, hora, notas, estado)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pendiente')
    ");
    $stmt->execute([$codigo, $restaurante_id, $nombre, $telefono, $correo ?: null, $num_personas, $fecha, $hora, $notas ?: null]);

    jsonResponse(['success' => true, 'id' => (int)$db->lastInsertId(), 'codigo' => $codigo]);
} catch (Exception $e) {
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
