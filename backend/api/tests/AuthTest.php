<?php

namespace Tests;

class AuthTest extends DatabaseTestCase
{
    public function test_login_correcto(): void
    {
        $res = $this->callScript('auth/auth_login.php', 'POST', [
            'email'    => self::TEST_EMAIL,
            'password' => self::TEST_PASSWORD,
        ]);

        $this->assertTrue($res['success'] ?? false, 'Login debería devolver success=true');
        $this->assertArrayHasKey('trabajador', $res);
        $this->assertEquals('admin', $res['trabajador']['rol']);
    }

    public function test_login_password_incorrecta(): void
    {
        $res = $this->callScript('auth/auth_login.php', 'POST', [
            'email'    => self::TEST_EMAIL,
            'password' => 'contrasenaMal',
        ]);

        $this->assertFalse($res['success'] ?? false);
        $this->assertArrayHasKey('error', $res);
    }

    public function test_login_sin_campos(): void
    {
        $res = $this->callScript('auth/auth_login.php', 'POST', []);

        $this->assertArrayHasKey('error', $res);
    }

    public function test_login_metodo_get_rechazado(): void
    {
        $res = $this->callScript('auth/auth_login.php', 'GET', []);

        $this->assertArrayHasKey('error', $res);
    }
}
