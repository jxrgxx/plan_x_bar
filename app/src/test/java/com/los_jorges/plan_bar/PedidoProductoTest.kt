package com.los_jorges.plan_bar

import com.los_jorges.plan_bar.model.PedidoProducto
import org.junit.Assert.*
import org.junit.Test

class PedidoProductoTest {

    // ── Creación básica ────────────────────────────────────────────────────────

    @Test
    fun `linea de pedido se crea correctamente`() {
        val linea = PedidoProducto(
            id = 1,
            cantidad = 2,
            precio_unitario = 8.50,
            observaciones = null,
            fecha_agregado = "2024-01-01 12:00:00",
            nombre = "Croquetas",
            categoria = "entrante"
        )
        assertEquals(1, linea.id)
        assertEquals(2, linea.cantidad)
        assertEquals(8.50, linea.precio_unitario, 0.001)
        assertEquals("Croquetas", linea.nombre)
    }

    // ── Estado por defecto ─────────────────────────────────────────────────────

    @Test
    fun `estado por defecto es vacio (recien añadido)`() {
        val linea = PedidoProducto(1, 1, 5.0, null, null, "Agua", "bebida")
        assertEquals("", linea.estado)
    }

    // ── Estados ───────────────────────────────────────────────────────────────

    @Test
    fun `estado en preparacion es correcto`() {
        val linea = PedidoProducto(1, 1, 5.0, null, null, "Agua", "bebida", estado = "en preparacion")
        assertEquals("en preparacion", linea.estado)
    }

    @Test
    fun `estado preparado es correcto`() {
        val linea = PedidoProducto(1, 1, 5.0, null, null, "Agua", "bebida", estado = "preparado")
        assertEquals("preparado", linea.estado)
    }

    @Test
    fun `producto nuevo no ha sido enviado a cocina`() {
        val linea = PedidoProducto(1, 1, 5.0, null, null, "Agua", "bebida")
        assertTrue(linea.estado == "")
    }

    @Test
    fun `producto en cocina no es nuevo`() {
        val linea = PedidoProducto(1, 1, 5.0, null, null, "Agua", "bebida", estado = "en preparacion")
        assertFalse(linea.estado == "")
    }

    @Test
    fun `producto preparado no es nuevo`() {
        val linea = PedidoProducto(1, 1, 5.0, null, null, "Agua", "bebida", estado = "preparado")
        assertFalse(linea.estado == "")
    }

    // ── Subtotal ───────────────────────────────────────────────────────────────

    @Test
    fun `subtotal de linea es cantidad por precio`() {
        val linea = PedidoProducto(1, 3, 8.50, null, null, "Croquetas", "entrante")
        val subtotal = linea.cantidad * linea.precio_unitario
        assertEquals(25.50, subtotal, 0.001)
    }

    @Test
    fun `subtotal con cantidad 1 es el precio unitario`() {
        val linea = PedidoProducto(1, 1, 12.0, null, null, "Entrecot", "segundo")
        assertEquals(12.0, linea.cantidad * linea.precio_unitario, 0.001)
    }

    // ── Observaciones ─────────────────────────────────────────────────────────

    @Test
    fun `observaciones pueden ser null`() {
        val linea = PedidoProducto(1, 1, 5.0, null, null, "Agua", "bebida")
        assertNull(linea.observaciones)
    }

    @Test
    fun `observaciones pueden tener texto`() {
        val linea = PedidoProducto(1, 1, 5.0, "sin sal", null, "Croquetas", "entrante")
        assertEquals("sin sal", linea.observaciones)
    }

    // ── Igualdad y copia ───────────────────────────────────────────────────────

    @Test
    fun `dos lineas iguales son iguales`() {
        val l1 = PedidoProducto(1, 2, 8.50, null, null, "Croquetas", "entrante")
        val l2 = PedidoProducto(1, 2, 8.50, null, null, "Croquetas", "entrante")
        assertEquals(l1, l2)
    }

    @Test
    fun `copy permite cambiar estado`() {
        val nueva = PedidoProducto(1, 1, 5.0, null, null, "Agua", "bebida", estado = "")
        val enPrep = nueva.copy(estado = "en preparacion")
        assertEquals("en preparacion", enPrep.estado)
        assertEquals(nueva.nombre, enPrep.nombre)
    }
}
