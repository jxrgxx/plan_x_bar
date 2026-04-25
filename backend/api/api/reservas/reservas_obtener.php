<?php
require_once '../config/db.php';

$restaurante_id = intval($_GET['restaurante_id'] ?? 1);
$fecha          = $_GET['fecha'] ?? date('Y-m-d');

$db   = getDB();
$stmt = $db->prepare("
    SELECT r.id, r.codigo, r.nombre, r.telefono, r.correo, r.num_personas,
           r.fecha, r.hora, r.estado, r.notas,
           GROUP_CONCAT(m.codigo ORDER BY m.codigo) AS mesas
    FROM Reservas r
    LEFT JOIN ReservaMesas rm ON rm.reserva_id = r.id
    LEFT JOIN Mesas m ON m.id = rm.mesa_id
    WHERE r.restaurante_id = ? AND r.fecha = ?
    GROUP BY r.id
    ORDER BY r.hora
");
$stmt->execute([$restaurante_id, $fecha]);
$reservas = $stmt->fetchAll();

jsonResponse(['reservas' => $reservas]);
