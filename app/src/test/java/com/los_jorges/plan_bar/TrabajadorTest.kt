package com.los_jorges.plan_bar

import com.los_jorges.plan_bar.model.Trabajador
import org.junit.Assert.*
import org.junit.Test

class TrabajadorTest {

    // ── Creación básica ────────────────────────────────────────────────────────

    @Test
    fun `trabajador se crea con los campos correctos`() {
        val t = Trabajador(
            id = 1,
            restaurante_id = 10,
            nombre = "Jorge",
            rol = "admin",
            email = "jorge@planbar.com"
        )
        assertEquals(1, t.id)
        assertEquals(10, t.restaurante_id)
        assertEquals("Jorge", t.nombre)
        assertEquals("admin", t.rol)
        assertEquals("jorge@planbar.com", t.email)
    }

    // ── Valores por defecto ────────────────────────────────────────────────────

    @Test
    fun `activo es true por defecto`() {
        val t = Trabajador(1, 10, "Ana", "camarero", "ana@test.com")
        assertTrue(t.activo)
    }

    @Test
    fun `tiene_pin es false por defecto`() {
        val t = Trabajador(1, 10, "Ana", "camarero", "ana@test.com")
        assertFalse(t.tiene_pin)
    }

    @Test
    fun `restaurante_nombre es null por defecto`() {
        val t = Trabajador(1, 10, "Ana", "camarero", "ana@test.com")
        assertNull(t.restaurante_nombre)
    }

    // ── Roles ──────────────────────────────────────────────────────────────────

    @Test
    fun `rol admin se reconoce correctamente`() {
        val t = Trabajador(1, 10, "Jorge", "admin", "jorge@test.com")
        assertEquals("admin", t.rol)
        assertTrue(t.rol == "admin")
    }

    @Test
    fun `rol camarero no es admin`() {
        val t = Trabajador(2, 10, "Ana", "camarero", "ana@test.com")
        assertFalse(t.rol == "admin")
    }

    @Test
    fun `rol cocina no es admin`() {
        val t = Trabajador(3, 10, "Luis", "cocina", "luis@test.com")
        assertFalse(t.rol == "admin")
    }

    // ── Igualdad y copia ───────────────────────────────────────────────────────

    @Test
    fun `dos trabajadores con los mismos datos son iguales`() {
        val t1 = Trabajador(1, 10, "Jorge", "admin", "jorge@test.com")
        val t2 = Trabajador(1, 10, "Jorge", "admin", "jorge@test.com")
        assertEquals(t1, t2)
    }

    @Test
    fun `trabajadores con distinto id no son iguales`() {
        val t1 = Trabajador(1, 10, "Jorge", "admin", "jorge@test.com")
        val t2 = Trabajador(2, 10, "Jorge", "admin", "jorge@test.com")
        assertNotEquals(t1, t2)
    }

    @Test
    fun `copy permite desactivar un trabajador`() {
        val activo = Trabajador(1, 10, "Ana", "camarero", "ana@test.com", activo = true)
        val inactivo = activo.copy(activo = false)
        assertFalse(inactivo.activo)
        assertEquals(activo.nombre, inactivo.nombre)
    }

    @Test
    fun `copy permite asignar pin`() {
        val sinPin = Trabajador(1, 10, "Ana", "camarero", "ana@test.com", tiene_pin = false)
        val conPin = sinPin.copy(tiene_pin = true)
        assertTrue(conPin.tiene_pin)
    }
}
