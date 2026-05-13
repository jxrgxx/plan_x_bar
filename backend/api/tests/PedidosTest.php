<?php

namespace Tests;

class PedidosTest extends DatabaseTestCase
{
    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Crea un pedido de prueba y devuelve su ID. */
    private function crearPedido(): int
    {
        $res = $this->callScript('pedidos/pedidos_crear.php', 'POST', [
            'restaurante_id' => self::$testRestauranteId,
            'mesa_id'        => self::$testMesaId,
            'trabajador_id'  => self::$testTrabajadorId,
        ]);

        $this->assertTrue($res['success'] ?? false, 'No se pudo crear el pedido de prueba');
        return (int) $res['pedido_id'];
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    public function test_crear_pedido(): void
    {
        $res = $this->callScript('pedidos/pedidos_crear.php', 'POST', [
            'restaurante_id' => self::$testRestauranteId,
            'mesa_id'        => self::$testMesaId,
            'trabajador_id'  => self::$testTrabajadorId,
        ]);

        $this->assertTrue($res['success'] ?? false);
        $this->assertArrayHasKey('pedido_id', $res);

        // La mesa debe quedar ocupada
        $stmt = self::$pdo->prepare("SELECT estado FROM Mesas WHERE id = ?");
        $stmt->execute([self::$testMesaId]);
        $this->assertEquals('ocupada', $stmt->fetchColumn());
    }

    public function test_crear_pedido_duplicado_falla(): void
    {
        $this->crearPedido();   // primer pedido

        $res = $this->callScript('pedidos/pedidos_crear.php', 'POST', [
            'restaurante_id' => self::$testRestauranteId,
            'mesa_id'        => self::$testMesaId,
        ]);

        $this->assertArrayHasKey('error', $res, 'Debe devolver error si la mesa ya tiene pedido abierto');
    }

    public function test_agregar_producto_al_pedido(): void
    {
        $pedidoId = $this->crearPedido();

        $res = $this->callScript('pedidos/pedidos_agregar_producto.php', 'POST', [
            'pedido_id'   => $pedidoId,
            'producto_id' => self::$testProductoId,
            'cantidad'    => 2,
        ]);

        $this->assertTrue($res['success'] ?? false);
        $this->assertEquals(1.50, $res['precio_unitario']);

        // Verificar en BD: 1 línea, cantidad 2
        $stmt = self::$pdo->prepare("SELECT COUNT(*) FROM PedidoProductos WHERE pedido_id = ?");
        $stmt->execute([$pedidoId]);
        $this->assertEquals(1, (int) $stmt->fetchColumn());
    }

    public function test_agregar_mismo_producto_acumula_cantidad(): void
    {
        $pedidoId = $this->crearPedido();

        $this->callScript('pedidos/pedidos_agregar_producto.php', 'POST', [
            'pedido_id'   => $pedidoId,
            'producto_id' => self::$testProductoId,
            'cantidad'    => 1,
        ]);
        $this->callScript('pedidos/pedidos_agregar_producto.php', 'POST', [
            'pedido_id'   => $pedidoId,
            'producto_id' => self::$testProductoId,
            'cantidad'    => 1,
        ]);

        // Debe haber 1 sola línea con cantidad = 2 (sin observaciones)
        $stmt = self::$pdo->prepare("SELECT cantidad FROM PedidoProductos WHERE pedido_id = ?");
        $stmt->execute([$pedidoId]);
        $this->assertEquals(2, (int) $stmt->fetchColumn());
    }

    public function test_cerrar_pedido(): void
    {
        $pedidoId = $this->crearPedido();
        $this->callScript('pedidos/pedidos_agregar_producto.php', 'POST', [
            'pedido_id'   => $pedidoId,
            'producto_id' => self::$testProductoId,
            'cantidad'    => 1,
        ]);

        $res = $this->callScript('pedidos/pedidos_cerrar.php', 'POST', [
            'pedido_id'   => $pedidoId,
            'metodo_pago' => 'efectivo',
        ]);

        $this->assertTrue($res['success'] ?? false);

        // El pedido debe estar en estado 'pagado'
        $stmt = self::$pdo->prepare("SELECT estado FROM Pedidos WHERE id = ?");
        $stmt->execute([$pedidoId]);
        $this->assertEquals('pagado', $stmt->fetchColumn());

        // La mesa debe quedar libre
        $stmt = self::$pdo->prepare("SELECT estado FROM Mesas WHERE id = ?");
        $stmt->execute([self::$testMesaId]);
        $this->assertEquals('libre', $stmt->fetchColumn());
    }

    public function test_cerrar_pedido_metodo_invalido_falla(): void
    {
        $pedidoId = $this->crearPedido();

        $res = $this->callScript('pedidos/pedidos_cerrar.php', 'POST', [
            'pedido_id'   => $pedidoId,
            'metodo_pago' => 'bitcoin',
        ]);

        $this->assertArrayHasKey('error', $res);
    }

    public function test_cancelar_pedido_vacio(): void
    {
        $pedidoId = $this->crearPedido();

        $res = $this->callScript('pedidos/pedidos_cancelar.php', 'POST', [
            'pedido_id' => $pedidoId,
        ]);

        $this->assertTrue($res['success'] ?? false);

        // La mesa debe quedar libre
        $stmt = self::$pdo->prepare("SELECT estado FROM Mesas WHERE id = ?");
        $stmt->execute([self::$testMesaId]);
        $this->assertEquals('libre', $stmt->fetchColumn());
    }
}
