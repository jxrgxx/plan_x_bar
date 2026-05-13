<?php

namespace Tests;

class MesasTest extends DatabaseTestCase
{
    public function test_obtener_mesas_del_restaurante(): void
    {
        $res = $this->callScript('mesas/mesas_obtener.php', 'GET', [
            'restaurante_id' => self::$testRestauranteId,
        ]);

        $this->assertArrayHasKey('mesas', $res);
        $this->assertCount(1, $res['mesas'], 'Debe haber exactamente 1 mesa de prueba');
        $this->assertEquals('T1', $res['mesas'][0]['codigo']);
    }

    public function test_crear_mesa(): void
    {
        $res = $this->callScript('mesas/mesas_crear.php', 'POST', [
            'restaurante_id' => self::$testRestauranteId,
            'codigo'         => 'T99',
            'capacidad'      => 6,
        ]);

        $this->assertTrue($res['success'] ?? false);

        // Verificar directamente en BD
        $stmt = self::$pdo->prepare("SELECT * FROM Mesas WHERE restaurante_id = ? AND codigo = 'T99'");
        $stmt->execute([self::$testRestauranteId]);
        $mesa = $stmt->fetch();
        $this->assertNotFalse($mesa, 'La mesa T99 debe existir en la BD');
        $this->assertEquals(6, (int) $mesa['capacidad']);

        // Limpiar la mesa extra creada en este test
        self::$pdo->prepare("DELETE FROM Mesas WHERE id = ?")->execute([$mesa['id']]);
    }

    public function test_crear_mesa_sin_codigo_falla(): void
    {
        $res = $this->callScript('mesas/mesas_crear.php', 'POST', [
            'restaurante_id' => self::$testRestauranteId,
            'capacidad'      => 4,
        ]);

        $this->assertArrayHasKey('error', $res);
    }
}
