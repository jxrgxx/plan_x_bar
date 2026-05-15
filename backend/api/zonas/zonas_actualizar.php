<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') jsonResponse(['error' => 'Método no permitido'], 405);

$body           = getBody();
$restaurante_id = (int)($body['restaurante_id'] ?? 0);
$zonas          = $body['zonas'] ?? [];

if (!$restaurante_id || !is_array($zonas) || empty($zonas)) {
    jsonResponse(['error' => 'Datos inválidos'], 400);
}

$db = getDB();

// ── Paso 1: validar antes de tocar nada ────────────────────────────────────
// Si se intenta desactivar una zona que tiene mesas, abortar con aviso
foreach ($zonas as $zona) {
    $id     = (int)($zona['id'] ?? 0);
    $activo = (int)(bool)($zona['activo'] ?? true);

    if ($id > 0 && $activo === 0) {
        $stmtCheck = $db->prepare("SELECT COUNT(*) FROM Mesas WHERE zona_id = ?");
        $stmtCheck->execute([$id]);
        $count = (int) $stmtCheck->fetchColumn();

        if ($count > 0) {
            $stmtNombre = $db->prepare("SELECT nombre FROM Zonas WHERE id = ?");
            $stmtNombre->execute([$id]);
            $nombre = $stmtNombre->fetchColumn() ?: "zona #$id";
            // HTTP 200 para que Retrofit deserialice el body correctamente
            jsonResponse([
                'success' => false,
                'error'   => "La zona \"$nombre\" tiene $count mesa(s). Elimínalas antes de desactivarla."
            ]);
        }
    }
}

// ── Paso 2: upsert ────────────────────────────────────────────────────────
$stmtUpd = $db->prepare(
    "UPDATE Zonas SET nombre = ?, activo = ? WHERE id = ? AND restaurante_id = ?"
);
$stmtIns = $db->prepare(
    "INSERT INTO Zonas (restaurante_id, clave, nombre, orden, activo) VALUES (?, ?, ?, ?, ?)"
);

foreach ($zonas as $zona) {
    $id     = (int)($zona['id']     ?? 0);
    $nombre = trim($zona['nombre']  ?? '');
    $activo = (int)(bool)($zona['activo'] ?? true);
    $clave  = trim($zona['clave']   ?? '');
    $orden  = (int)($zona['orden']  ?? 1);

    if (!$nombre) continue;

    if ($id > 0) {
        $stmtUpd->execute([$nombre, $activo, $id, $restaurante_id]);
    } elseif ($clave) {
        // Zona nueva (sin id): comprobar si ya existe por clave para no duplicar
        $stmtExiste = $db->prepare("SELECT id FROM Zonas WHERE restaurante_id = ? AND clave = ?");
        $stmtExiste->execute([$restaurante_id, $clave]);
        $existente = $stmtExiste->fetch();
        if ($existente) {
            $stmtUpd->execute([$nombre, $activo, $existente['id'], $restaurante_id]);
        } else {
            $stmtIns->execute([$restaurante_id, $clave, $nombre, $orden, $activo]);
        }
    }
}

jsonResponse(['success' => true]);
