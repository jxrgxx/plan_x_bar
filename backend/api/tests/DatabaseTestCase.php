<?php

namespace Tests;

use PDO;
use PHPUnit\Framework\TestCase;

/**
 * Clase base para tests de integración.
 *
 * - Crea un restaurante de prueba (y sus trabajadores, mesas y productos)
 *   antes de la primera prueba del grupo.
 * - Limpia pedidos y restablece el estado de las mesas antes de cada test.
 * - Borra todos los datos de prueba al terminar el grupo.
 */
abstract class DatabaseTestCase extends TestCase
{
    protected static PDO $pdo;
    protected static int $testRestauranteId;
    protected static int $testTrabajadorId;
    protected static int $testMesaId;
    protected static int $testProductoId;

    // ── Credenciales de la BD local (XAMPP) ───────────────────────────────────
    private const DB_HOST    = '127.0.0.1';
    private const DB_NAME    = 'plan_bar_pruebas_php';
    private const DB_USER    = 'root';
    private const DB_PASS    = '';
    private const DB_CHARSET = 'utf8mb4';

    // ── Datos de prueba ────────────────────────────────────────────────────────
    protected const TEST_EMAIL    = 'test_planbar@example.com';
    protected const TEST_PASSWORD = 'test1234';

    // ─────────────────────────────────────────────────────────────────────────
    //  Setup / Teardown del grupo
    // ─────────────────────────────────────────────────────────────────────────

    public static function setUpBeforeClass(): void
    {
        self::$pdo = new PDO(
            'mysql:host=' . self::DB_HOST . ';dbname=' . self::DB_NAME . ';charset=' . self::DB_CHARSET,
            self::DB_USER,
            self::DB_PASS,
            [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            ]
        );

        // Limpiar restos de ejecuciones anteriores
        $old = self::$pdo->prepare("SELECT id FROM Restaurante WHERE email = ?");
        $old->execute([self::TEST_EMAIL]);
        $oldId = $old->fetchColumn();
        if ($oldId) {
            self::$pdo->exec("DELETE pp FROM PedidoProductos pp JOIN Pedidos p ON p.id = pp.pedido_id WHERE p.restaurante_id = $oldId");
            self::$pdo->exec("DELETE FROM Pedidos      WHERE restaurante_id = $oldId");
            self::$pdo->exec("DELETE FROM Productos    WHERE restaurante_id = $oldId");
            self::$pdo->exec("DELETE FROM Mesas        WHERE restaurante_id = $oldId");
            self::$pdo->exec("DELETE FROM Trabajadores WHERE restaurante_id = $oldId");
            self::$pdo->exec("DELETE FROM Restaurante  WHERE id = $oldId");
        }

        // Restaurante de prueba
        self::$pdo->prepare(
            "INSERT INTO Restaurante (nombre, email, direccion, telefono)
             VALUES ('TEST_PLANBAR', ?, 'Calle Test 1', '000000000')"
        )->execute([self::TEST_EMAIL]);
        self::$testRestauranteId = (int) self::$pdo->lastInsertId();

        // Trabajador admin de prueba
        $hash = password_hash(self::TEST_PASSWORD, PASSWORD_BCRYPT);
        self::$pdo->prepare(
            "INSERT INTO Trabajadores (restaurante_id, nombre, rol, email, password_hash, activo)
             VALUES (?, 'Test Admin', 'admin', ?, ?, 1)"
        )->execute([self::$testRestauranteId, self::TEST_EMAIL, $hash]);
        self::$testTrabajadorId = (int) self::$pdo->lastInsertId();

        // Mesa de prueba
        self::$pdo->prepare(
            "INSERT INTO Mesas (restaurante_id, codigo, capacidad, estado)
             VALUES (?, 'T1', 4, 'libre')"
        )->execute([self::$testRestauranteId]);
        self::$testMesaId = (int) self::$pdo->lastInsertId();

        // Producto de prueba
        self::$pdo->prepare(
            "INSERT INTO Productos (restaurante_id, nombre, categoria, descripcion, precio, disponible)
             VALUES (?, 'Agua', 'bebida', 'Agua mineral', 1.50, 1)"
        )->execute([self::$testRestauranteId]);
        self::$testProductoId = (int) self::$pdo->lastInsertId();
    }

    public static function tearDownAfterClass(): void
    {
        $rid = self::$testRestauranteId;

        // Borrar en orden para respetar FK
        self::$pdo->exec(
            "DELETE pp FROM PedidoProductos pp
             JOIN Pedidos p ON p.id = pp.pedido_id
             WHERE p.restaurante_id = $rid"
        );
        self::$pdo->exec("DELETE FROM Pedidos    WHERE restaurante_id = $rid");
        self::$pdo->exec("DELETE FROM Productos  WHERE restaurante_id = $rid");
        self::$pdo->exec("DELETE FROM Mesas      WHERE restaurante_id = $rid");
        self::$pdo->exec("DELETE FROM Trabajadores WHERE restaurante_id = $rid");
        self::$pdo->exec("DELETE FROM Restaurante  WHERE id = $rid");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Setup de cada test
    // ─────────────────────────────────────────────────────────────────────────

    protected function setUp(): void
    {
        $rid = self::$testRestauranteId;

        // Limpiar pedidos anteriores
        self::$pdo->exec(
            "DELETE pp FROM PedidoProductos pp
             JOIN Pedidos p ON p.id = pp.pedido_id
             WHERE p.restaurante_id = $rid"
        );
        self::$pdo->exec("DELETE FROM Pedidos WHERE restaurante_id = $rid");

        // Resetear mesa a libre
        self::$pdo->exec("UPDATE Mesas SET estado = 'libre' WHERE restaurante_id = $rid");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helper principal
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ejecuta uno de los scripts PHP de la API y devuelve el JSON decodificado.
     *
     * @param string $relPath  Ruta relativa desde backend/api/api/  (ej: 'pedidos/pedidos_crear.php')
     * @param string $method   Método HTTP a simular ('GET' | 'POST')
     * @param array  $body     Cuerpo de la petición
     */
    protected function callScript(string $relPath, string $method = 'POST', array $body = []): array
    {
        $scriptPath = realpath(__DIR__ . '/../api/' . $relPath);
        $dir        = dirname($scriptPath);

        $prevCwd = getcwd();
        chdir($dir);

        $_SERVER['REQUEST_METHOD'] = $method;
        $_ENV['TEST_BODY']         = json_encode($body);

        // Scripts GET leen de $_GET en vez de getBody()
        $prevGet = $_GET;
        if ($method === 'GET') $_GET = array_merge($_GET, $body);

        ob_start();
        try {
            include $scriptPath;   // include (no require_once) para reutilizar el script en varios tests
        } catch (\JsonResponseException $e) {
            // jsonResponse() lanza esta excepción en test mode para detener la ejecución del script
        }
        $out = ob_get_clean();

        chdir($prevCwd);
        $_SERVER['REQUEST_METHOD'] = 'GET';
        $_ENV['TEST_BODY']         = '{}';
        $_GET                      = $prevGet;

        return json_decode($out, true) ?? [];
    }
}
