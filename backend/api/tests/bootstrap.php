<?php
// Modo test: jsonResponse() no llama a exit y getBody() lee de $_ENV['TEST_BODY']
$_ENV['TEST_MODE'] = '1';
$_ENV['TEST_BODY'] = '{}';
$_SERVER['REQUEST_METHOD'] = 'GET';

// BD local de XAMPP para tests
$_ENV['TEST_DB_HOST'] = '127.0.0.1';
$_ENV['TEST_DB_NAME'] = 'plan_bar_pruebas_php';
$_ENV['TEST_DB_USER'] = 'root';
$_ENV['TEST_DB_PASS'] = '';
