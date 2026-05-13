<?php
define('DB_HOST',    $_ENV['TEST_DB_HOST']    ?? 'pdb1037.awardspace.net');
define('DB_NAME',    $_ENV['TEST_DB_NAME']    ?? '4742077_planbar');
define('DB_USER',    $_ENV['TEST_DB_USER']    ?? '4742077_planbar');
define('DB_PASS',    $_ENV['TEST_DB_PASS']    ?? 'p2Sm8uc/r_W/iaX');
define('DB_CHARSET', 'utf8mb4');

function getDB(): PDO {
    static $pdo = null;
    if ($pdo === null) {
        $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . DB_CHARSET;
        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        ];
        try {
            $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
        } catch (PDOException $e) {
            http_response_code(500);
            echo json_encode(['error' => 'Error de conexión a la base de datos']);
            exit;
        }
    }
    return $pdo;
}

class JsonResponseException extends RuntimeException {
    public function __construct(public readonly array $payload, public readonly int $statusCode) {
        parent::__construct(json_encode($payload));
    }
}

function jsonResponse(mixed $data, int $code = 200): void {
    http_response_code($code);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    if (!empty($_ENV['TEST_MODE'])) throw new JsonResponseException($data, $code);
    exit;
}

function getBody(): array {
    if (!empty($_ENV['TEST_BODY'])) return json_decode($_ENV['TEST_BODY'], true) ?? [];
    return json_decode(file_get_contents('php://input'), true) ?? [];
}

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') exit(0);