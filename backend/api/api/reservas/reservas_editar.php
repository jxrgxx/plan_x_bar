<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['success' => false, 'error' => 'Método no permitido'], 405);

$body = getBody();
$id   = (int)($body['id'] ?? 0);
if (!$id) jsonResponse(['success' => false, 'error' => 'id requerido']);

$estadosValidos = ['pendiente', 'confirmada', 'cancelada', 'completada'];

// Construir SET dinámico con los campos que vengan en el body
$campos = [];
$valores = [];

if (isset($body['estado'])) {
    if (!in_array($body['estado'], $estadosValidos))
        jsonResponse(['success' => false, 'error' => 'Estado no válido']);
    $campos[] = 'estado = ?';
    $valores[] = trim($body['estado']);
}
if (isset($body['nombre']))      { $campos[] = 'nombre = ?';      $valores[] = trim($body['nombre']); }
if (isset($body['telefono']))    { $campos[] = 'telefono = ?';    $valores[] = trim($body['telefono']); }
if (isset($body['correo']))      { $campos[] = 'correo = ?';      $valores[] = trim($body['correo']) ?: null; }
if (isset($body['num_personas'])){ $campos[] = 'num_personas = ?';$valores[] = (int)$body['num_personas']; }
if (isset($body['hora']))        { $campos[] = 'hora = ?';        $valores[] = trim($body['hora']); }
if (isset($body['notas']))       { $campos[] = 'notas = ?';       $valores[] = trim($body['notas']) ?: null; }

if (empty($campos)) jsonResponse(['success' => false, 'error' => 'Nada que actualizar']);

$valores[] = $id;

try {
    $db   = getDB();
    $sql  = "UPDATE Reservas SET " . implode(', ', $campos) . " WHERE id = ?";
    $stmt = $db->prepare($sql);
    $stmt->execute($valores);
    jsonResponse(['success' => true]);
} catch (Exception $e) {
    jsonResponse(['success' => false, 'error' => $e->getMessage()], 500);
}
