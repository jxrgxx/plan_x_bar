package com.los_jorges.plan_bar

import com.los_jorges.plan_bar.model.Producto
import org.junit.Assert.*
import org.junit.Test

class ProductoTest {

    // ── Creación básica ────────────────────────────────────────────────────────

    @Test
    fun `producto se crea con los campos correctos`() {
        val p = Producto(
            id = 1,
            restaurante_id = 10,
            nombre = "Croquetas",
            categoria = "entrante",
            descripcion = "Croquetas de jamón",
            precio = 8.50,
            disponible = true
        )
        assertEquals(1, p.id)
        assertEquals("Croquetas", p.nombre)
        assertEquals("entrante", p.categoria)
        assertEquals(8.50, p.precio, 0.001)
        assertTrue(p.disponible)
    }

    // ── Precio ─────────────────────────────────────────────────────────────────

    @Test
    fun `precio de cero es válido`() {
        val p = Producto(1, 10, "Agua", "bebida", "", 0.0, true)
        assertEquals(0.0, p.precio, 0.001)
    }

    @Test
    fun `precio negativo se almacena tal cual`() {
        val p = Producto(1, 10, "Error", "bebida", "", -1.0, true)
        assertTrue(p.precio < 0)
    }

    @Test
    fun `precio con decimales se conserva`() {
        val p = Producto(1, 10, "Vino", "bebida", "", 12.75, true)
        assertEquals(12.75, p.precio, 0.001)
    }

    // ── Disponibilidad ─────────────────────────────────────────────────────────

    @Test
    fun `producto disponible puede pedirse`() {
        val p = Producto(1, 10, "Croquetas", "entrante", "", 8.50, true)
        assertTrue(p.disponible)
    }

    @Test
    fun `producto no disponible no puede pedirse`() {
        val p = Producto(1, 10, "Croquetas", "entrante", "", 8.50, false)
        assertFalse(p.disponible)
    }

    @Test
    fun `copy permite desactivar disponibilidad`() {
        val disponible = Producto(1, 10, "Croquetas", "entrante", "", 8.50, true)
        val noDisponible = disponible.copy(disponible = false)
        assertFalse(noDisponible.disponible)
        assertEquals(disponible.nombre, noDisponible.nombre)
    }

    // ── Categorías ─────────────────────────────────────────────────────────────

    @Test
    fun `categorias validas reconocidas`() {
        val categorias = listOf("bebida", "entrante", "primero", "segundo", "postre")
        categorias.forEach { cat ->
            val p = Producto(1, 10, "Test", cat, "", 5.0, true)
            assertEquals(cat, p.categoria)
        }
    }

    // ── Igualdad ───────────────────────────────────────────────────────────────

    @Test
    fun `dos productos iguales son iguales`() {
        val p1 = Producto(1, 10, "Croquetas", "entrante", "", 8.50, true)
        val p2 = Producto(1, 10, "Croquetas", "entrante", "", 8.50, true)
        assertEquals(p1, p2)
    }

    @Test
    fun `productos con distinto precio no son iguales`() {
        val p1 = Producto(1, 10, "Croquetas", "entrante", "", 8.50, true)
        val p2 = Producto(1, 10, "Croquetas", "entrante", "", 9.00, true)
        assertNotEquals(p1, p2)
    }
}
