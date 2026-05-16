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
        val linea = PedidoProducto(id = 1, cantidad = 1, precio_unitario = 5.0, observaciones = null, fecha_agregado = null, nombre = "Agua", categoria = "bebida")
        assertEquals("", linea.estado)
    }

    // ── Estados ───────────────────────────────────────────────────────────────

    @Test
    fun `estado en preparacion es correcto`() {
        val linea = PedidoProducto(id = 1, cantidad = 1, precio_unitario = 5.0, observaciones = null, fecha_agregado = null, nombre = "Agua", categoria = "bebida", estado = "en preparacion")
        assertEquals("en preparacion", linea.estado)
    }

    @Test
    fun `estado preparado es correcto`() {
        val linea = PedidoProducto(id = 1, cantidad = 1, precio_unitario = 5.0, observaciones = null, fecha_agregado = null, nombre = "Agua", categoria = "bebida", estado = "preparado")
        assertEquals("preparado", linea.estado)
    }

    @Test
    fun `producto nuevo no ha sido enviado a cocina`() {
        val linea = PedidoProducto(id = 1, cantidad = 1, precio_unitario = 5.0, observaciones = null, fecha_agregado = null, nombre = "Agua", categoria = "bebida")
        assertTrue(linea.estado == "")
    }

    @Test
    fun `producto en cocina no es nuevo`() {
        val linea = PedidoProducto(id = 1, cantidad = 1, precio_unitario = 5.0, observaciones = null, fecha_agregado = null, nombre = "Agua", categoria = "bebida", estado = "en preparacion")
        assertFalse(linea.estado == "")
    }

    @Test
    fun `producto preparado no es nuevo`() {
        val linea = PedidoProducto(id = 1, cantidad = 1, precio_unitario = 5.0, observaciones = null, fecha_agregado = null, nombre = "Agua", categoria = "bebida", estado = "preparado")
        assertFalse(linea.estado == "")
    }

    // ── Subtotal ───────────────────────────────────────────────────────────────

    @Test
    fun `subtotal de linea es cantidad por precio`() {
        val linea = PedidoProducto(id = 1, cantidad = 3, precio_unitario = 8.50, observaciones = null, fecha_agregado = null, nombre = "Croquetas", categoria = "entrante")
        val subtotal = linea.cantidad * linea.precio_unitario
        assertEquals(25.50, subtotal, 0.001)
    }

    @Test
    fun `subtotal con cantidad 1 es el precio unitario`() {
        val linea = PedidoProducto(id = 1, cantidad = 1, precio_unitario = 12.0, observaciones = null, fecha_agregado = null, nombre = "Entrecot", categoria = "segundo")
        assertEquals(12.0, linea.cantidad * linea.precio_unitario, 0.001)
    }

    // ── Observaciones ─────────────────────────────────────────────────────────

    @Test
    fun `observaciones pueden ser null`() {
        val linea = PedidoProducto(id = 1, cantidad = 1, precio_unitario = 5.0, observaciones = null, fecha_agregado = null, nombre = "Agua", categoria = "bebida")
        assertNull(linea.observaciones)
    }

    @Test
    fun `observaciones pueden tener texto`() {
        val linea = PedidoProducto(id = 1, cantidad = 1, precio_unitario = 5.0, observaciones = "sin sal", fecha_agregado = null, nombre = "Croquetas", categoria = "entrante")
        assertEquals("sin sal", linea.observaciones)
    }

    // ── Igualdad y copia ───────────────────────────────────────────────────────

    @Test
    fun `dos lineas iguales son iguales`() {
        val l1 = PedidoProducto(id = 1, cantidad = 2, precio_unitario = 8.50, observaciones = null, fecha_agregado = null, nombre = "Croquetas", categoria = "entrante")
        val l2 = PedidoProducto(id = 1, cantidad = 2, precio_unitario = 8.50, observaciones = null, fecha_agregado = null, nombre = "Croquetas", categoria = "entrante")
        assertEquals(l1, l2)
    }

    @Test
    fun `copy permite cambiar estado`() {
        val nueva = PedidoProducto(id = 1, cantidad = 1, precio_unitario = 5.0, observaciones = null, fecha_agregado = null, nombre = "Agua", categoria = "bebida", estado = "")
        val enPrep = nueva.copy(estado = "en preparacion")
        assertEquals("en preparacion", enPrep.estado)
        assertEquals(nueva.nombre, enPrep.nombre)
    }
}
