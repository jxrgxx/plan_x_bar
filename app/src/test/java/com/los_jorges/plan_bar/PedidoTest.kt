package com.los_jorges.plan_bar

import com.los_jorges.plan_bar.model.Pedido
import com.los_jorges.plan_bar.model.PedidoProducto
import org.junit.Assert.*
import org.junit.Test

class PedidoTest {

    private fun linea(
        id: Int,
        nombre: String,
        cantidad: Int = 1,
        precio: Double = 10.0,
        estado: String = ""
    ) = PedidoProducto(id, cantidad, precio, null, null, nombre, "entrante", estado)

    // ── Creación básica ────────────────────────────────────────────────────────

    @Test
    fun `pedido se crea con campos correctos`() {
        val pedido = Pedido(
            id = 1,
            estado = "abierto",
            fecha_apertura = "2024-01-01 12:00:00",
            total = 25.0,
            trabajador_id = 5,
            mesa_id = 3,
            mesa_codigo = "M3"
        )
        assertEquals(1, pedido.id)
        assertEquals("abierto", pedido.estado)
        assertEquals(3, pedido.mesa_id)
        assertEquals("M3", pedido.mesa_codigo)
    }

    @Test
    fun `pedido nuevo tiene lista de productos vacia`() {
        val pedido = Pedido(1, "abierto", null, null, null, 1, "M1")
        assertTrue(pedido.productos.isEmpty())
    }

    // ── Estados ───────────────────────────────────────────────────────────────

    @Test
    fun `estado abierto es reconocido`() {
        val pedido = Pedido(1, "abierto", null, null, null, 1, "M1")
        assertEquals("abierto", pedido.estado)
    }

    @Test
    fun `estado en_cocina es reconocido`() {
        val pedido = Pedido(1, "en_cocina", null, null, null, 1, "M1")
        assertEquals("en_cocina", pedido.estado)
    }

    @Test
    fun `estado pagado es reconocido`() {
        val pedido = Pedido(1, "pagado", null, null, null, 1, "M1")
        assertEquals("pagado", pedido.estado)
    }

    @Test
    fun `pedido abierto no esta pagado`() {
        val pedido = Pedido(1, "abierto", null, null, null, 1, "M1")
        assertFalse(pedido.estado == "pagado")
    }

    // ── Productos ─────────────────────────────────────────────────────────────

    @Test
    fun `pedido con productos no esta vacio`() {
        val pedido = Pedido(
            id = 1, estado = "abierto", fecha_apertura = null,
            total = null, trabajador_id = null, mesa_id = 1, mesa_codigo = "M1",
            productos = listOf(linea(1, "Croquetas"))
        )
        assertFalse(pedido.productos.isEmpty())
    }

    @Test
    fun `pedido detecta productos nuevos sin enviar a cocina`() {
        val pedido = Pedido(
            id = 1, estado = "en_cocina", fecha_apertura = null,
            total = null, trabajador_id = null, mesa_id = 1, mesa_codigo = "M1",
            productos = listOf(
                linea(1, "Croquetas", estado = "en preparacion"),
                linea(2, "Agua", estado = "")  // nuevo sin enviar
            )
        )
        val hayNuevos = pedido.productos.any { it.estado == "" }
        assertTrue(hayNuevos)
    }

    @Test
    fun `pedido sin productos nuevos no debe mostrar enviar a cocina`() {
        val pedido = Pedido(
            id = 1, estado = "en_cocina", fecha_apertura = null,
            total = null, trabajador_id = null, mesa_id = 1, mesa_codigo = "M1",
            productos = listOf(
                linea(1, "Croquetas", estado = "en preparacion"),
                linea(2, "Agua", estado = "preparado")
            )
        )
        val hayNuevos = pedido.productos.any { it.estado == "" }
        assertFalse(hayNuevos)
    }

    @Test
    fun `todos los productos preparados indica pedido listo`() {
        val pedido = Pedido(
            id = 1, estado = "en_cocina", fecha_apertura = null,
            total = null, trabajador_id = null, mesa_id = 1, mesa_codigo = "M1",
            productos = listOf(
                linea(1, "Croquetas", estado = "preparado"),
                linea(2, "Entrecot", estado = "preparado")
            )
        )
        val todoListo = pedido.productos.all { it.estado == "preparado" }
        assertTrue(todoListo)
    }

    // ── Total ──────────────────────────────────────────────────────────────────

    @Test
    fun `total puede ser null en pedido recien creado`() {
        val pedido = Pedido(1, "abierto", null, null, null, 1, "M1")
        assertNull(pedido.total)
    }

    @Test
    fun `total con valor se conserva`() {
        val pedido = Pedido(1, "pagado", null, 45.50, null, 1, "M1")
        assertEquals(45.50, pedido.total!!, 0.001)
    }

    // ── Igualdad y copia ───────────────────────────────────────────────────────

    @Test
    fun `dos pedidos con los mismos datos son iguales`() {
        val p1 = Pedido(1, "abierto", null, null, null, 1, "M1")
        val p2 = Pedido(1, "abierto", null, null, null, 1, "M1")
        assertEquals(p1, p2)
    }

    @Test
    fun `copy permite cambiar estado del pedido`() {
        val abierto = Pedido(1, "abierto", null, null, null, 1, "M1")
        val enCocina = abierto.copy(estado = "en_cocina")
        assertEquals("en_cocina", enCocina.estado)
        assertEquals(abierto.id, enCocina.id)
    }
}
