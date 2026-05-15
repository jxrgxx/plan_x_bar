package com.los_jorges.plan_bar.ui.theme

import androidx.compose.runtime.compositionLocalOf

interface AppStrings {
    // ── Ajustes ───────────────────────────────────────────────────────────────
    val ajustes: String
    val apariencia: String
    val modoOscuro: String
    val modoClaroLabel: String
    val cambiaTema: String
    val idioma: String
    val sonidoPedido: String
    val suenaPedido: String
    val volver: String

    // ── Comunes ───────────────────────────────────────────────────────────────
    val guardar: String
    val cancelar: String
    val eliminar: String
    val editar: String
    val confirmar: String
    val cerrar: String
    val aplicar: String
    val anadir: String
    val enviar: String
    val crear: String
    val aceptar: String
    val nombre: String

    // ── Login ─────────────────────────────────────────────────────────────────
    val loginSubtitulo: String
    val emailLabel: String
    val contrasena: String
    val entrar: String
    val sinCuentaRegistra: String

    // ── Registro ──────────────────────────────────────────────────────────────
    val registro: String
    val creaLaCuenta: String
    val datosDelRestaurante: String
    val nombreRestaurante: String
    val emailRestaurante: String
    val direccion: String
    val telefono: String
    val cuentaDeAdministrador: String
    val nombreDelAdministrador: String
    val emailAdmin: String
    val contrasenaAdmin: String
    val registrarRestaurante: String
    val yaTienesCuenta: String

    // ── Selector Personal ─────────────────────────────────────────────────────
    val seleccionaTuPerfil: String
    val verReservas: String
    val nuevaReserva: String
    val cerrarSesion: String
    val introducirContrasenaAdmin: String
    val reservasDeHoy: String
    val sinReservasHoy: String
    val anadirAlRestaurante: String
    val datosDelCliente: String
    val detallesReserva: String
    val crearReserva: String
    val seleccionarFecha: String
    val seleccionarLaHora: String
    val comentarioOpcional: String
    val rolAdmin: String
    val rolCocina: String
    val rolCamarero: String
    val desmarcarLlegado: String
    val marcarComoLlegado: String

    // ── Admin Panel ───────────────────────────────────────────────────────────
    val panelDeGestion: String
    val queQuieresGestionar: String
    val mesas: String
    val disposicionSalon: String
    val productos: String
    val gestionarLaCarta: String
    val trabajadores: String
    val gestionarElEquipo: String
    val reservas: String
    val verYGestionarReservas: String
    val estadisticas: String
    val ventasPlatosYMas: String
    val menuDelDia: String
    val configurarOfertaDiaria: String
    val espaciosDeTrabajo: String
    val numeroYNombresZonas: String

    // ── Mesas Admin ───────────────────────────────────────────────────────────
    val mesaLabel: String
    val nuevaMesa: String
    val editarMesa: String
    val eliminarMesa: String
    val anadirNuevaMesa: String
    val sinMesasEnZona: String
    val codigoMesa: String
    val capacidadPersonas: String
    val espacio: String
    val estado: String
    val verPlano: String
    val modificando: String
    val confirmarEliminarMesa: String
    val personasLabel: String
    val mesaCreada: String
    val mesaActualizada: String
    val mesaEliminadaOk: String

    // ── Productos Admin ───────────────────────────────────────────────────────
    val gestionaLaCarta: String
    val nuevoProducto: String
    val editarProducto: String
    val eliminarProducto: String
    val noDisponible: String
    val nombreObligatorio: String
    val categoria: String
    val descripcion: String
    val precioEur: String
    val disponibleEnCarta: String
    val anadirPlatoOBebida: String
    val sinProductosPulsaPlus: String
    val confirmarEliminarProducto: String
    val productoCreado: String
    val productoActualizado: String
    val productoEliminado: String

    // ── Trabajadores Admin ────────────────────────────────────────────────────
    val gestionaElEquipo: String
    val nuevoTrabajador: String
    val editarTrabajador: String
    val eliminarTrabajador: String
    val inactivo: String
    val pinOpcional: String
    val nuevoPinOpcional: String
    val maxSeisDig: String
    val trabajadorActivo: String
    val rol: String
    val anadirMiembro: String
    val sinTrabajadoresPulsaPlus: String
    val emailObligatorio: String
    val confirmarEliminarTrabajador: String
    val trabajadorCreado: String
    val trabajadorActualizado: String
    val trabajadorEliminado: String

    // ── Reservas Admin ────────────────────────────────────────────────────────
    val sinReservasEsteDia: String
    val anadirReserva: String
    val editarReserva: String
    val eliminarReserva: String
    val nuevaReservaAdmin: String
    val anadirReservaManualmente: String
    val nombreCliente: String
    val telefonoCliente: String
    val emailOpcional: String
    val personas: String
    val hora: String
    val notasOpcional: String
    val diaAnterior: String
    val diaSiguiente: String
    val reservaEliminada: String
    val reservaCreadaCodigo: String
    val reservaActualizada: String
    val confirmarEliminarReserva: String
    val errorActualizarEstado: String

    // ── Estadísticas ──────────────────────────────────────────────────────────
    val panelDelJefe: String
    val sinDatosParaElPeriodo: String
    val ventas: String
    val servicio: String
    val comensales: String
    val paxTotales: String
    val tiempoMedio: String
    val porMesa: String
    val descuentosLabel: String
    val aplicados: String
    val cortesias: String
    val invitadas: String
    val top3Platos: String
    val uds: String
    val porCategoria: String
    val porMesero: String
    val hoy: String
    val semana: String
    val mes: String
    val ventaBruta: String
    val ventaNeta: String
    val delBruto: String
    val comandas: String

    // ── Espacios Admin ────────────────────────────────────────────────────────
    val zonasYDistribucion: String
    val numeroDeEspacios: String
    val hasta4Zonas: String
    val nombresDeLosEspacios: String
    val personalizaCadaZona: String
    val configuracionGuardada: String
    val guardarConfiguracion: String
    val noSePuedeGuardar: String
    val entendido: String
    val cambiosSincronizan: String

    // ── Plano Mesas ───────────────────────────────────────────────────────────
    val planoDelRestaurante: String
    val nuevoElemento: String
    val elementoCreado: String
    val eliminado: String
    val usaElBotonParaAnadir: String
    val tocaUnElemento: String
    val eliminarElemento: String
    val tipoElemento: String
    val color: String
    val dimensionesYRotacion: String
    val ancho: String
    val alto: String
    val rotacion: String

    // ── Mesas Camarero ────────────────────────────────────────────────────────
    val hola: String
    val cambiarTrabajador: String
    val cuantosComensales: String
    val unoPorDefecto: String
    val abrirMesa: String

    // ── Cocina ────────────────────────────────────────────────────────────────
    val cocina: String
    val todoAlDia: String
    val salir: String
    val sinPedidosEnCocina: String
    val actualizandoCada5s: String
    val ahora: String
    val progreso: String
    val platosLabel: String
    val pedidoEnMarcha: String
    val pedidosEnMarcha: String
    val estadoListo: String
    val estadoCancelado: String
    val estadoPreparado: String
    val estadoPendiente: String

    // ── Menú del Día Admin ────────────────────────────────────────────────────
    val configurarOfertaHoy: String
    val introducePrecioValido: String
    val menuGuardado: String
    val guardarMenu: String
    val precioDelMenu: String
    val pulsaPlusParaAnadir: String
    val sinProductosEnCategoria: String
    val udsLabel: String
    val seleccionaProductoYUnidades: String
    val cantidadDisponible: String
    val dejarVacioSinLimite: String
    val stockHint: String
    val noHayMasProductosCat: String
    val productoLabel: String
    val errorAlGuardar: String
    val anadirLabel: String

    // ── Comanda ───────────────────────────────────────────────────────────────
    val cancelarPedido: String
    val enviarACocina: String
    val sinPedidoAbierto: String
    val sinProductosAun: String
    val enviaProductosAntesDeCobrar: String
    val cobrar: String
    val menuDelDiaLabel: String
    val seleccionDelDia: String
    val agotado: String
    val restantes: String
    val disponibles: String
    val comandarMenuDelDia: String
    val modificar: String
    val eliminarMenu: String
    val quitarTodosProductosMenu: String
    val modificarMenu: String
    val sinAnadir: String
    val noAnadir: String
    val guardarCambios: String
    val cuantosMenus: String
    val nDeMenus: String
    val empezar: String
    val menuDelDiaAnadido: String
    val quitarDelPedido: String
    val quitar: String
    val mantener: String
    val cancelarPedidoTitulo: String
    val seCancelaraElPedido: String
    val productosSinEnviar: String
    val tieneProductosSinEnviar: String
    val enviarYSalir: String
    val salirSinEnviar: String
    val enviadoACocina: String
    val enviarACocinaConfirm: String
    val seEnviaranPendientes: String
    val pedidoEnviadoACocina: String
    val descuento: String
    val tipoDeDescuento: String
    val porcentaje: String
    val fijoEur: String
    val importeEur: String
    val quitarDescuento: String
    val noHayBebidasDisponibles: String
    val cobrarPedido: String
    val seleccionaMetodoPago: String
    val totalACobrar: String
    val metodoDePago: String
    val desmarcarServida: String
    val marcarServida: String
    val marcarComoServida: String
    val masOpciones: String
    val totalLabel: String
    val anadidoFmt: String
    val agotadoFmt: String
    val deLabel: String
    val seEliminaDelPedidoFmt: String
    val menosLabel: String
    val masLabel: String
}

object EsStrings : AppStrings {
    // ── Ajustes ───────────────────────────────────────────────────────────────
    override val ajustes = "Ajustes"
    override val apariencia = "Apariencia"
    override val modoOscuro = "Modo oscuro"
    override val modoClaroLabel = "Modo claro"
    override val cambiaTema = "Cambia el tema visual de la app"
    override val idioma = "Idioma"
    override val sonidoPedido = "Sonido pedido nuevo"
    override val suenaPedido = "Suena al llegar un pedido a cocina"
    override val volver = "Volver"

    // ── Comunes ───────────────────────────────────────────────────────────────
    override val guardar = "Guardar"
    override val cancelar = "Cancelar"
    override val eliminar = "Eliminar"
    override val editar = "Editar"
    override val confirmar = "Confirmar"
    override val cerrar = "Cerrar"
    override val aplicar = "Aplicar"
    override val anadir = "Añadir"
    override val enviar = "Enviar"
    override val crear = "Crear"
    override val aceptar = "Aceptar"
    override val nombre = "Nombre"

    // ── Login ─────────────────────────────────────────────────────────────────
    override val loginSubtitulo = "Tu restaurante, bajo control"
    override val emailLabel = "Email"
    override val contrasena = "Contraseña"
    override val entrar = "Entrar"
    override val sinCuentaRegistra = "¿Sin cuenta? Registra tu restaurante"

    // ── Registro ──────────────────────────────────────────────────────────────
    override val registro = "Registro"
    override val creaLaCuenta = "Crea la cuenta de tu restaurante"
    override val datosDelRestaurante = "Datos del restaurante"
    override val nombreRestaurante = "Nombre *"
    override val emailRestaurante = "Email del restaurante *"
    override val direccion = "Dirección"
    override val telefono = "Teléfono"
    override val cuentaDeAdministrador = "Cuenta de administrador"
    override val nombreDelAdministrador = "Nombre del administrador *"
    override val emailAdmin = "Email *"
    override val contrasenaAdmin = "Contraseña *"
    override val registrarRestaurante = "Registrar restaurante"
    override val yaTienesCuenta = "¿Ya tienes cuenta? Inicia sesión"

    // ── Selector Personal ─────────────────────────────────────────────────────
    override val seleccionaTuPerfil = "Selecciona tu perfil"
    override val verReservas = "Ver reservas"
    override val nuevaReserva = "Nueva reserva"
    override val cerrarSesion = "Cerrar sesión"
    override val introducirContrasenaAdmin = "Introduce la contraseña del administrador"
    override val reservasDeHoy = "Reservas de hoy"
    override val sinReservasHoy = "Sin reservas hoy"
    override val anadirAlRestaurante = "Añade una reserva al restaurante"
    override val datosDelCliente = "Datos del cliente"
    override val detallesReserva = "Detalles de la reserva"
    override val crearReserva = "Crear reserva"
    override val seleccionarFecha = "Selecciona fecha"
    override val seleccionarLaHora = "Selecciona la hora"
    override val comentarioOpcional = "Comentario (opcional)"
    override val rolAdmin = "Admin"
    override val rolCocina = "Cocina"
    override val rolCamarero = "Camarero"
    override val desmarcarLlegado = "Desmarcar"
    override val marcarComoLlegado = "Marcar como llegado"

    // ── Admin Panel ───────────────────────────────────────────────────────────
    override val panelDeGestion = "Panel de gestión"
    override val queQuieresGestionar = "¿Qué quieres gestionar?"
    override val mesas = "Mesas"
    override val disposicionSalon = "Disposición y estado del salón"
    override val productos = "Productos"
    override val gestionarLaCarta = "Gestionar la carta"
    override val trabajadores = "Trabajadores"
    override val gestionarElEquipo = "Gestionar el equipo"
    override val reservas = "Reservas"
    override val verYGestionarReservas = "Ver y gestionar reservas por día"
    override val estadisticas = "Estadísticas"
    override val ventasPlatosYMas = "Ventas, platos, meseros y más"
    override val menuDelDia = "Menú del Día"
    override val configurarOfertaDiaria = "Configura la oferta diaria"
    override val espaciosDeTrabajo = "Espacios de trabajo"
    override val numeroYNombresZonas = "Número y nombres de las zonas del local"

    // ── Mesas Admin ───────────────────────────────────────────────────────────
    override val mesaLabel = "Mesa"
    override val nuevaMesa = "Nueva mesa"
    override val editarMesa = "Editar mesa"
    override val eliminarMesa = "Eliminar mesa"
    override val anadirNuevaMesa = "Añade una nueva mesa al salón"
    override val sinMesasEnZona = "No hay mesas en esta zona. Pulsa + para añadir."
    override val codigoMesa = "Código (ej: M01)"
    override val capacidadPersonas = "Capacidad (personas)"
    override val espacio = "Espacio"
    override val estado = "Estado"
    override val verPlano = "Ver plano"
    override val modificando = "Modificando"
    override val confirmarEliminarMesa = "¿Eliminar la mesa"
    override val personasLabel = "personas"
    override val mesaCreada = "Mesa creada"
    override val mesaActualizada = "Mesa actualizada"
    override val mesaEliminadaOk = "Mesa eliminada"

    // ── Productos Admin ───────────────────────────────────────────────────────
    override val gestionaLaCarta = "Gestiona la carta"
    override val nuevoProducto = "Nuevo producto"
    override val editarProducto = "Editar producto"
    override val eliminarProducto = "Eliminar producto"
    override val noDisponible = "No disponible"
    override val nombreObligatorio = "Nombre *"
    override val categoria = "Categoría"
    override val descripcion = "Descripción"
    override val precioEur = "Precio (€) *"
    override val disponibleEnCarta = "Disponible en carta"
    override val anadirPlatoOBebida = "Añade un plato o bebida a la carta"
    override val sinProductosPulsaPlus = "No hay productos. Pulsa + para añadir."
    override val confirmarEliminarProducto = "¿Eliminar"
    override val productoCreado = "Producto creado"
    override val productoActualizado = "Producto actualizado"
    override val productoEliminado = "Producto eliminado"

    // ── Trabajadores Admin ────────────────────────────────────────────────────
    override val gestionaElEquipo = "Gestiona el equipo"
    override val nuevoTrabajador = "Nuevo trabajador"
    override val editarTrabajador = "Editar trabajador"
    override val eliminarTrabajador = "Eliminar trabajador"
    override val inactivo = "Inactivo"
    override val pinOpcional = "PIN (opcional)"
    override val nuevoPinOpcional = "Nuevo PIN (opcional)"
    override val maxSeisDig = "Máx. 6 dígitos"
    override val trabajadorActivo = "Trabajador activo"
    override val rol = "Rol"
    override val anadirMiembro = "Añade un miembro al equipo"
    override val sinTrabajadoresPulsaPlus = "No hay trabajadores. Pulsa + para añadir."
    override val emailObligatorio = "Email *"
    override val confirmarEliminarTrabajador = "¿Eliminar a"
    override val trabajadorCreado = "Trabajador creado"
    override val trabajadorActualizado = "Trabajador actualizado"
    override val trabajadorEliminado = "Trabajador eliminado"

    // ── Reservas Admin ────────────────────────────────────────────────────────
    override val sinReservasEsteDia = "Sin reservas este día"
    override val anadirReserva = "Añadir reserva"
    override val editarReserva = "Editar reserva"
    override val eliminarReserva = "Eliminar reserva"
    override val nuevaReservaAdmin = "Nueva reserva"
    override val anadirReservaManualmente = "Añade una reserva manualmente"
    override val nombreCliente = "Nombre *"
    override val telefonoCliente = "Teléfono *"
    override val emailOpcional = "Email (opcional)"
    override val personas = "Personas *"
    override val hora = "Hora *"
    override val notasOpcional = "Notas (opcional)"
    override val diaAnterior = "Día anterior"
    override val diaSiguiente = "Día siguiente"
    override val reservaEliminada = "Reserva eliminada"
    override val reservaCreadaCodigo = "Reserva creada · Código:"
    override val reservaActualizada = "Reserva actualizada"
    override val confirmarEliminarReserva = "¿Eliminar la reserva de"
    override val errorActualizarEstado = "Error al actualizar estado"

    // ── Estadísticas ──────────────────────────────────────────────────────────
    override val panelDelJefe = "Panel del jefe"
    override val sinDatosParaElPeriodo = "Sin datos para el período"
    override val ventas = "Ventas"
    override val servicio = "Servicio"
    override val comensales = "Comensales"
    override val paxTotales = "pax totales"
    override val tiempoMedio = "T. medio"
    override val porMesa = "por mesa"
    override val descuentosLabel = "Descuentos"
    override val aplicados = "aplicados"
    override val cortesias = "Cortesías"
    override val invitadas = "invitadas"
    override val top3Platos = "Top 3 platos"
    override val uds = "uds"
    override val porCategoria = "Por categoría"
    override val porMesero = "Por mesero"
    override val hoy = "Hoy"
    override val semana = "Semana"
    override val mes = "Mes"
    override val ventaBruta = "Venta Bruta"
    override val ventaNeta = "Venta Neta"
    override val delBruto = "del bruto"
    override val comandas = "comandas"

    // ── Espacios Admin ────────────────────────────────────────────────────────
    override val zonasYDistribucion = "Zonas y distribución del local"
    override val numeroDeEspacios = "Número de espacios"
    override val hasta4Zonas = "Hasta 4 zonas distintas en el local"
    override val nombresDeLosEspacios = "Nombres de los espacios"
    override val personalizaCadaZona = "Personaliza cada zona"
    override val configuracionGuardada = "¡Configuración guardada!"
    override val guardarConfiguracion = "Guardar configuración"
    override val noSePuedeGuardar = "No se puede guardar"
    override val entendido = "Entendido"
    override val cambiosSincronizan = "Los cambios se sincronizan en todos los dispositivos del restaurante."

    // ── Plano Mesas ───────────────────────────────────────────────────────────
    override val planoDelRestaurante = "Plano del restaurante"
    override val nuevoElemento = "Nuevo elemento"
    override val elementoCreado = "Elemento creado"
    override val eliminado = "Eliminado"
    override val usaElBotonParaAnadir = "Usa el botón + para añadir elementos y mesas"
    override val tocaUnElemento = "Toca un elemento para seleccionarlo"
    override val eliminarElemento = "Eliminar elemento"
    override val tipoElemento = "Pared, barra, columna…"
    override val color = "Color"
    override val dimensionesYRotacion = "Dimensiones y rotación"
    override val ancho = "Ancho"
    override val alto = "Alto"
    override val rotacion = "Rotación (°)"

    // ── Mesas Camarero ────────────────────────────────────────────────────────
    override val hola = "Hola,"
    override val cambiarTrabajador = "Cambiar trabajador"
    override val cuantosComensales = "¿Cuántos comensales?"
    override val unoPorDefecto = "1 (por defecto)"
    override val abrirMesa = "Abrir mesa"

    // ── Cocina ────────────────────────────────────────────────────────────────
    override val cocina = "Cocina"
    override val todoAlDia = "Todo al día"
    override val salir = "Salir"
    override val sinPedidosEnCocina = "Sin pedidos en cocina"
    override val actualizandoCada5s = "Actualizando cada 5 segundos…"
    override val ahora = "Ahora"
    override val progreso = "Progreso"
    override val platosLabel = "platos"
    override val pedidoEnMarcha = "pedido en marcha"
    override val pedidosEnMarcha = "pedidos en marcha"
    override val estadoListo = "LISTO"
    override val estadoCancelado = "CANCELADO"
    override val estadoPreparado = "PREPARADO"
    override val estadoPendiente = "PENDIENTE"

    // ── Menú del Día Admin ────────────────────────────────────────────────────
    override val configurarOfertaHoy = "Configura la oferta de hoy"
    override val introducePrecioValido = "Introduce un precio válido"
    override val menuGuardado = "Menú guardado ✓"
    override val guardarMenu = "Guardar menú"
    override val precioDelMenu = "Precio del menú (€)"
    override val pulsaPlusParaAnadir = "Pulsa + para añadir opciones"
    override val sinProductosEnCategoria = "Sin productos en esta categoría"
    override val udsLabel = "Uds."
    override val seleccionaProductoYUnidades = "Selecciona producto y unidades disponibles"
    override val cantidadDisponible = "Cantidad disponible"
    override val dejarVacioSinLimite = "Dejar vacío = sin límite"
    override val stockHint = "-1 = sin límite. 0 = agotado. Cualquier otro número = stock disponible."
    override val noHayMasProductosCat = "No hay más productos de esta categoría"
    override val productoLabel = "Producto"
    override val errorAlGuardar = "Error al guardar"
    override val anadirLabel = "Añadir"

    // ── Comanda ───────────────────────────────────────────────────────────────
    override val cancelarPedido = "Cancelar pedido"
    override val enviarACocina = "Enviar a cocina"
    override val sinPedidoAbierto = "Sin pedido abierto"
    override val sinProductosAun = "Sin productos aún"
    override val enviaProductosAntesDeCobrar = "Envía los productos a cocina antes de cobrar"
    override val cobrar = "Cobrar"
    override val menuDelDiaLabel = "Menú del día"
    override val seleccionDelDia = "Selección del día"
    override val agotado = "Agotado"
    override val restantes = "restantes"
    override val disponibles = "disponibles"
    override val comandarMenuDelDia = "Comandar menú del día"
    override val modificar = "Modificar"
    override val eliminarMenu = "Eliminar menú"
    override val quitarTodosProductosMenu = "¿Quitar todos los productos de este menú del pedido?"
    override val modificarMenu = "Modificar menú"
    override val sinAnadir = "sin añadir"
    override val noAnadir = "No añadir"
    override val guardarCambios = "Guardar cambios"
    override val cuantosMenus = "¿Cuántos menús?"
    override val nDeMenus = "Nº de menús"
    override val empezar = "Empezar"
    override val menuDelDiaAnadido = "Menú del día añadido"
    override val quitarDelPedido = "¿Quitar del pedido?"
    override val quitar = "Quitar"
    override val mantener = "Mantener"
    override val cancelarPedidoTitulo = "¿Cancelar pedido?"
    override val seCancelaraElPedido = "Se cancelará el pedido y la mesa quedará libre."
    override val productosSinEnviar = "Productos sin enviar"
    override val tieneProductosSinEnviar = "Tienes productos que aún no han ido a cocina. ¿Qué quieres hacer?"
    override val enviarYSalir = "Enviar y salir"
    override val salirSinEnviar = "Salir sin enviar"
    override val enviadoACocina = "Enviado a cocina"
    override val enviarACocinaConfirm = "¿Enviar a cocina?"
    override val seEnviaranPendientes = "Se enviarán todos los productos pendientes a cocina."
    override val pedidoEnviadoACocina = "Pedido enviado a cocina"
    override val descuento = "Descuento"
    override val tipoDeDescuento = "Tipo de descuento"
    override val porcentaje = "Porcentaje (%)"
    override val fijoEur = "Fijo (€)"
    override val importeEur = "Importe (€)"
    override val quitarDescuento = "Quitar descuento"
    override val noHayBebidasDisponibles = "No hay bebidas disponibles en la carta."
    override val cobrarPedido = "Cobrar pedido"
    override val seleccionaMetodoPago = "Selecciona el método de pago"
    override val totalACobrar = "Total a cobrar"
    override val metodoDePago = "Método de pago"
    override val desmarcarServida = "Desmarcar servida"
    override val marcarServida = "Marcar servida"
    override val marcarComoServida = "Marcar como servida"
    override val masOpciones = "Más opciones"
    override val totalLabel = "Total"
    override val anadidoFmt = "%s añadido"
    override val agotadoFmt = "%s agotado"
    override val deLabel = "de"
    override val seEliminaDelPedidoFmt = "Se eliminará \"%s\" del pedido."
    override val menosLabel = "Menos"
    override val masLabel = "Más"
}

object EnStrings : AppStrings {
    // ── Ajustes ───────────────────────────────────────────────────────────────
    override val ajustes = "Settings"
    override val apariencia = "Appearance"
    override val modoOscuro = "Dark mode"
    override val modoClaroLabel = "Light mode"
    override val cambiaTema = "Change the app visual theme"
    override val idioma = "Language"
    override val sonidoPedido = "New order sound"
    override val suenaPedido = "Plays when a new order arrives"
    override val volver = "Back"

    // ── Comunes ───────────────────────────────────────────────────────────────
    override val guardar = "Save"
    override val cancelar = "Cancel"
    override val eliminar = "Delete"
    override val editar = "Edit"
    override val confirmar = "Confirm"
    override val cerrar = "Close"
    override val aplicar = "Apply"
    override val anadir = "Add"
    override val enviar = "Send"
    override val crear = "Create"
    override val aceptar = "Accept"
    override val nombre = "Name"

    // ── Login ─────────────────────────────────────────────────────────────────
    override val loginSubtitulo = "Your restaurant, under control"
    override val emailLabel = "Email"
    override val contrasena = "Password"
    override val entrar = "Sign in"
    override val sinCuentaRegistra = "No account? Register your restaurant"

    // ── Registro ──────────────────────────────────────────────────────────────
    override val registro = "Sign up"
    override val creaLaCuenta = "Create your restaurant account"
    override val datosDelRestaurante = "Restaurant details"
    override val nombreRestaurante = "Name *"
    override val emailRestaurante = "Restaurant email *"
    override val direccion = "Address"
    override val telefono = "Phone"
    override val cuentaDeAdministrador = "Administrator account"
    override val nombreDelAdministrador = "Administrator name *"
    override val emailAdmin = "Email *"
    override val contrasenaAdmin = "Password *"
    override val registrarRestaurante = "Register restaurant"
    override val yaTienesCuenta = "Already have an account? Sign in"

    // ── Selector Personal ─────────────────────────────────────────────────────
    override val seleccionaTuPerfil = "Select your profile"
    override val verReservas = "View reservations"
    override val nuevaReserva = "New reservation"
    override val cerrarSesion = "Sign out"
    override val introducirContrasenaAdmin = "Enter the administrator password"
    override val reservasDeHoy = "Today's reservations"
    override val sinReservasHoy = "No reservations today"
    override val anadirAlRestaurante = "Add a reservation to the restaurant"
    override val datosDelCliente = "Customer details"
    override val detallesReserva = "Reservation details"
    override val crearReserva = "Create reservation"
    override val seleccionarFecha = "Select date"
    override val seleccionarLaHora = "Select time"
    override val comentarioOpcional = "Comment (optional)"
    override val rolAdmin = "Admin"
    override val rolCocina = "Kitchen"
    override val rolCamarero = "Waiter"
    override val desmarcarLlegado = "Unmark"
    override val marcarComoLlegado = "Mark as arrived"

    // ── Admin Panel ───────────────────────────────────────────────────────────
    override val panelDeGestion = "Management panel"
    override val queQuieresGestionar = "What do you want to manage?"
    override val mesas = "Tables"
    override val disposicionSalon = "Layout and status of the dining room"
    override val productos = "Products"
    override val gestionarLaCarta = "Manage the menu"
    override val trabajadores = "Staff"
    override val gestionarElEquipo = "Manage the team"
    override val reservas = "Reservations"
    override val verYGestionarReservas = "View and manage daily reservations"
    override val estadisticas = "Statistics"
    override val ventasPlatosYMas = "Sales, dishes, waiters and more"
    override val menuDelDia = "Daily Menu"
    override val configurarOfertaDiaria = "Configure the daily offer"
    override val espaciosDeTrabajo = "Work areas"
    override val numeroYNombresZonas = "Number and names of venue areas"

    // ── Mesas Admin ───────────────────────────────────────────────────────────
    override val mesaLabel = "Table"
    override val nuevaMesa = "New table"
    override val editarMesa = "Edit table"
    override val eliminarMesa = "Delete table"
    override val anadirNuevaMesa = "Add a new table to the dining room"
    override val sinMesasEnZona = "No tables in this area. Tap + to add."
    override val codigoMesa = "Code (e.g. T01)"
    override val capacidadPersonas = "Capacity (persons)"
    override val espacio = "Area"
    override val estado = "Status"
    override val verPlano = "View floor plan"
    override val modificando = "Editing"
    override val confirmarEliminarMesa = "Delete table"
    override val personasLabel = "persons"
    override val mesaCreada = "Table created"
    override val mesaActualizada = "Table updated"
    override val mesaEliminadaOk = "Table deleted"

    // ── Productos Admin ───────────────────────────────────────────────────────
    override val gestionaLaCarta = "Manage the menu"
    override val nuevoProducto = "New product"
    override val editarProducto = "Edit product"
    override val eliminarProducto = "Delete product"
    override val noDisponible = "Not available"
    override val nombreObligatorio = "Name *"
    override val categoria = "Category"
    override val descripcion = "Description"
    override val precioEur = "Price (€) *"
    override val disponibleEnCarta = "Available on menu"
    override val anadirPlatoOBebida = "Add a dish or drink to the menu"
    override val sinProductosPulsaPlus = "No products. Tap + to add."
    override val confirmarEliminarProducto = "Delete"
    override val productoCreado = "Product created"
    override val productoActualizado = "Product updated"
    override val productoEliminado = "Product deleted"

    // ── Trabajadores Admin ────────────────────────────────────────────────────
    override val gestionaElEquipo = "Manage the team"
    override val nuevoTrabajador = "New staff member"
    override val editarTrabajador = "Edit staff member"
    override val eliminarTrabajador = "Delete staff member"
    override val inactivo = "Inactive"
    override val pinOpcional = "PIN (optional)"
    override val nuevoPinOpcional = "New PIN (optional)"
    override val maxSeisDig = "Max. 6 digits"
    override val trabajadorActivo = "Active staff"
    override val rol = "Role"
    override val anadirMiembro = "Add a member to the team"
    override val sinTrabajadoresPulsaPlus = "No staff. Tap + to add."
    override val emailObligatorio = "Email *"
    override val confirmarEliminarTrabajador = "Delete staff member"
    override val trabajadorCreado = "Staff member created"
    override val trabajadorActualizado = "Staff member updated"
    override val trabajadorEliminado = "Staff member deleted"

    // ── Reservas Admin ────────────────────────────────────────────────────────
    override val sinReservasEsteDia = "No reservations this day"
    override val anadirReserva = "Add reservation"
    override val editarReserva = "Edit reservation"
    override val eliminarReserva = "Delete reservation"
    override val nuevaReservaAdmin = "New reservation"
    override val anadirReservaManualmente = "Add a reservation manually"
    override val nombreCliente = "Name *"
    override val telefonoCliente = "Phone *"
    override val emailOpcional = "Email (optional)"
    override val personas = "Persons *"
    override val hora = "Time *"
    override val notasOpcional = "Notes (optional)"
    override val diaAnterior = "Previous day"
    override val diaSiguiente = "Next day"
    override val reservaEliminada = "Reservation deleted"
    override val reservaCreadaCodigo = "Reservation created · Code:"
    override val reservaActualizada = "Reservation updated"
    override val confirmarEliminarReserva = "Delete reservation for"
    override val errorActualizarEstado = "Error updating status"

    // ── Estadísticas ──────────────────────────────────────────────────────────
    override val panelDelJefe = "Boss panel"
    override val sinDatosParaElPeriodo = "No data for the period"
    override val ventas = "Sales"
    override val servicio = "Service"
    override val comensales = "Diners"
    override val paxTotales = "total pax"
    override val tiempoMedio = "Avg. time"
    override val porMesa = "per table"
    override val descuentosLabel = "Discounts"
    override val aplicados = "applied"
    override val cortesias = "Complimentary"
    override val invitadas = "given"
    override val top3Platos = "Top 3 dishes"
    override val uds = "units"
    override val porCategoria = "By category"
    override val porMesero = "By waiter"
    override val hoy = "Today"
    override val semana = "Week"
    override val mes = "Month"
    override val ventaBruta = "Gross Sales"
    override val ventaNeta = "Net Sales"
    override val delBruto = "of gross"
    override val comandas = "orders"

    // ── Espacios Admin ────────────────────────────────────────────────────────
    override val zonasYDistribucion = "Areas and venue layout"
    override val numeroDeEspacios = "Number of areas"
    override val hasta4Zonas = "Up to 4 different areas in the venue"
    override val nombresDeLosEspacios = "Area names"
    override val personalizaCadaZona = "Customize each area"
    override val configuracionGuardada = "Configuration saved!"
    override val guardarConfiguracion = "Save configuration"
    override val noSePuedeGuardar = "Cannot save"
    override val entendido = "Got it"
    override val cambiosSincronizan = "Changes sync to all restaurant devices."

    // ── Plano Mesas ───────────────────────────────────────────────────────────
    override val planoDelRestaurante = "Restaurant floor plan"
    override val nuevoElemento = "New element"
    override val elementoCreado = "Element created"
    override val eliminado = "Deleted"
    override val usaElBotonParaAnadir = "Use the + button to add elements and tables"
    override val tocaUnElemento = "Tap an element to select it"
    override val eliminarElemento = "Delete element"
    override val tipoElemento = "Wall, bar, column…"
    override val color = "Color"
    override val dimensionesYRotacion = "Dimensions and rotation"
    override val ancho = "Width"
    override val alto = "Height"
    override val rotacion = "Rotation (°)"

    // ── Mesas Camarero ────────────────────────────────────────────────────────
    override val hola = "Hi,"
    override val cambiarTrabajador = "Change staff"
    override val cuantosComensales = "How many diners?"
    override val unoPorDefecto = "1 (default)"
    override val abrirMesa = "Open table"

    // ── Cocina ────────────────────────────────────────────────────────────────
    override val cocina = "Kitchen"
    override val todoAlDia = "All caught up"
    override val salir = "Exit"
    override val sinPedidosEnCocina = "No orders in kitchen"
    override val actualizandoCada5s = "Updating every 5 seconds…"
    override val ahora = "Now"
    override val progreso = "Progress"
    override val platosLabel = "dishes"
    override val pedidoEnMarcha = "order in progress"
    override val pedidosEnMarcha = "orders in progress"
    override val estadoListo = "READY"
    override val estadoCancelado = "CANCELLED"
    override val estadoPreparado = "PREPARED"
    override val estadoPendiente = "PENDING"

    // ── Menú del Día Admin ────────────────────────────────────────────────────
    override val configurarOfertaHoy = "Configure today's offer"
    override val introducePrecioValido = "Enter a valid price"
    override val menuGuardado = "Menu saved ✓"
    override val guardarMenu = "Save menu"
    override val precioDelMenu = "Menu price (€)"
    override val pulsaPlusParaAnadir = "Tap + to add options"
    override val sinProductosEnCategoria = "No products in this category"
    override val udsLabel = "Units"
    override val seleccionaProductoYUnidades = "Select product and available units"
    override val cantidadDisponible = "Available quantity"
    override val dejarVacioSinLimite = "Leave empty = no limit"
    override val stockHint = "-1 = no limit. 0 = sold out. Any other number = available stock."
    override val noHayMasProductosCat = "No more products in this category"
    override val productoLabel = "Product"
    override val errorAlGuardar = "Error saving"
    override val anadirLabel = "Add"

    // ── Comanda ───────────────────────────────────────────────────────────────
    override val cancelarPedido = "Cancel order"
    override val enviarACocina = "Send to kitchen"
    override val sinPedidoAbierto = "No open order"
    override val sinProductosAun = "No products yet"
    override val enviaProductosAntesDeCobrar = "Send products to kitchen before charging"
    override val cobrar = "Charge"
    override val menuDelDiaLabel = "Daily menu"
    override val seleccionDelDia = "Today's selection"
    override val agotado = "Sold out"
    override val restantes = "remaining"
    override val disponibles = "available"
    override val comandarMenuDelDia = "Order daily menu"
    override val modificar = "Modify"
    override val eliminarMenu = "Delete menu"
    override val quitarTodosProductosMenu = "Remove all products from this menu in the order?"
    override val modificarMenu = "Modify menu"
    override val sinAnadir = "not added"
    override val noAnadir = "Don't add"
    override val guardarCambios = "Save changes"
    override val cuantosMenus = "How many menus?"
    override val nDeMenus = "No. of menus"
    override val empezar = "Start"
    override val menuDelDiaAnadido = "Daily menu added"
    override val quitarDelPedido = "Remove from order?"
    override val quitar = "Remove"
    override val mantener = "Keep"
    override val cancelarPedidoTitulo = "Cancel order?"
    override val seCancelaraElPedido = "The order will be cancelled and the table will be freed."
    override val productosSinEnviar = "Unsent products"
    override val tieneProductosSinEnviar = "You have products that haven't been sent to the kitchen yet. What do you want to do?"
    override val enviarYSalir = "Send and exit"
    override val salirSinEnviar = "Exit without sending"
    override val enviadoACocina = "Sent to kitchen"
    override val enviarACocinaConfirm = "Send to kitchen?"
    override val seEnviaranPendientes = "All pending products will be sent to the kitchen."
    override val pedidoEnviadoACocina = "Order sent to kitchen"
    override val descuento = "Discount"
    override val tipoDeDescuento = "Discount type"
    override val porcentaje = "Percentage (%)"
    override val fijoEur = "Fixed (€)"
    override val importeEur = "Amount (€)"
    override val quitarDescuento = "Remove discount"
    override val noHayBebidasDisponibles = "No drinks available on the menu."
    override val cobrarPedido = "Charge order"
    override val seleccionaMetodoPago = "Select payment method"
    override val totalACobrar = "Total to charge"
    override val metodoDePago = "Payment method"
    override val desmarcarServida = "Unmark served"
    override val marcarServida = "Mark served"
    override val marcarComoServida = "Mark as served"
    override val masOpciones = "More options"
    override val totalLabel = "Total"
    override val anadidoFmt = "%s added"
    override val agotadoFmt = "%s out of stock"
    override val deLabel = "of"
    override val seEliminaDelPedidoFmt = "\"%s\" will be removed from the order."
    override val menosLabel = "Less"
    override val masLabel = "More"
}

object FrStrings : AppStrings {
    // ── Ajustes ───────────────────────────────────────────────────────────────
    override val ajustes = "Paramètres"
    override val apariencia = "Apparence"
    override val modoOscuro = "Mode sombre"
    override val modoClaroLabel = "Mode clair"
    override val cambiaTema = "Changer le thème visuel de l'app"
    override val idioma = "Langue"
    override val sonidoPedido = "Son nouvelle commande"
    override val suenaPedido = "Joue quand une commande arrive"
    override val volver = "Retour"

    // ── Comunes ───────────────────────────────────────────────────────────────
    override val guardar = "Enregistrer"
    override val cancelar = "Annuler"
    override val eliminar = "Supprimer"
    override val editar = "Modifier"
    override val confirmar = "Confirmer"
    override val cerrar = "Fermer"
    override val aplicar = "Appliquer"
    override val anadir = "Ajouter"
    override val enviar = "Envoyer"
    override val crear = "Créer"
    override val aceptar = "Accepter"
    override val nombre = "Nom"

    // ── Login ─────────────────────────────────────────────────────────────────
    override val loginSubtitulo = "Votre restaurant, sous contrôle"
    override val emailLabel = "Email"
    override val contrasena = "Mot de passe"
    override val entrar = "Connexion"
    override val sinCuentaRegistra = "Pas de compte ? Inscrivez votre restaurant"

    // ── Registro ──────────────────────────────────────────────────────────────
    override val registro = "Inscription"
    override val creaLaCuenta = "Créez le compte de votre restaurant"
    override val datosDelRestaurante = "Données du restaurant"
    override val nombreRestaurante = "Nom *"
    override val emailRestaurante = "Email du restaurant *"
    override val direccion = "Adresse"
    override val telefono = "Téléphone"
    override val cuentaDeAdministrador = "Compte administrateur"
    override val nombreDelAdministrador = "Nom de l'administrateur *"
    override val emailAdmin = "Email *"
    override val contrasenaAdmin = "Mot de passe *"
    override val registrarRestaurante = "Inscrire le restaurant"
    override val yaTienesCuenta = "Déjà un compte ? Connectez-vous"

    // ── Selector Personal ─────────────────────────────────────────────────────
    override val seleccionaTuPerfil = "Sélectionnez votre profil"
    override val verReservas = "Voir les réservations"
    override val nuevaReserva = "Nouvelle réservation"
    override val cerrarSesion = "Déconnexion"
    override val introducirContrasenaAdmin = "Entrez le mot de passe administrateur"
    override val reservasDeHoy = "Réservations du jour"
    override val sinReservasHoy = "Aucune réservation aujourd'hui"
    override val anadirAlRestaurante = "Ajouter une réservation au restaurant"
    override val datosDelCliente = "Données du client"
    override val detallesReserva = "Détails de la réservation"
    override val crearReserva = "Créer une réservation"
    override val seleccionarFecha = "Sélectionner une date"
    override val seleccionarLaHora = "Sélectionner l'heure"
    override val comentarioOpcional = "Commentaire (optionnel)"
    override val rolAdmin = "Admin"
    override val rolCocina = "Cuisine"
    override val rolCamarero = "Serveur"
    override val desmarcarLlegado = "Décocher"
    override val marcarComoLlegado = "Marquer comme arrivé"

    // ── Admin Panel ───────────────────────────────────────────────────────────
    override val panelDeGestion = "Panneau de gestion"
    override val queQuieresGestionar = "Que voulez-vous gérer ?"
    override val mesas = "Tables"
    override val disposicionSalon = "Disposition et état de la salle"
    override val productos = "Produits"
    override val gestionarLaCarta = "Gérer le menu"
    override val trabajadores = "Personnel"
    override val gestionarElEquipo = "Gérer l'équipe"
    override val reservas = "Réservations"
    override val verYGestionarReservas = "Voir et gérer les réservations journalières"
    override val estadisticas = "Statistiques"
    override val ventasPlatosYMas = "Ventes, plats, serveurs et plus"
    override val menuDelDia = "Menu du Jour"
    override val configurarOfertaDiaria = "Configurer l'offre quotidienne"
    override val espaciosDeTrabajo = "Espaces de travail"
    override val numeroYNombresZonas = "Nombre et noms des zones du local"

    // ── Mesas Admin ───────────────────────────────────────────────────────────
    override val mesaLabel = "Table"
    override val nuevaMesa = "Nouvelle table"
    override val editarMesa = "Modifier la table"
    override val eliminarMesa = "Supprimer la table"
    override val anadirNuevaMesa = "Ajouter une nouvelle table à la salle"
    override val sinMesasEnZona = "Pas de tables dans cette zone. Appuyez sur + pour en ajouter."
    override val codigoMesa = "Code (ex : T01)"
    override val capacidadPersonas = "Capacité (personnes)"
    override val espacio = "Espace"
    override val estado = "Statut"
    override val verPlano = "Voir le plan"
    override val modificando = "Modification de"
    override val confirmarEliminarMesa = "Supprimer la table"
    override val personasLabel = "personnes"
    override val mesaCreada = "Table créée"
    override val mesaActualizada = "Table mise à jour"
    override val mesaEliminadaOk = "Table supprimée"

    // ── Productos Admin ───────────────────────────────────────────────────────
    override val gestionaLaCarta = "Gérer le menu"
    override val nuevoProducto = "Nouveau produit"
    override val editarProducto = "Modifier le produit"
    override val eliminarProducto = "Supprimer le produit"
    override val noDisponible = "Non disponible"
    override val nombreObligatorio = "Nom *"
    override val categoria = "Catégorie"
    override val descripcion = "Description"
    override val precioEur = "Prix (€) *"
    override val disponibleEnCarta = "Disponible au menu"
    override val anadirPlatoOBebida = "Ajouter un plat ou une boisson au menu"
    override val sinProductosPulsaPlus = "Aucun produit. Appuyez sur + pour en ajouter."
    override val confirmarEliminarProducto = "Supprimer"
    override val productoCreado = "Produit créé"
    override val productoActualizado = "Produit mis à jour"
    override val productoEliminado = "Produit supprimé"

    // ── Trabajadores Admin ────────────────────────────────────────────────────
    override val gestionaElEquipo = "Gérer l'équipe"
    override val nuevoTrabajador = "Nouveau membre"
    override val editarTrabajador = "Modifier le membre"
    override val eliminarTrabajador = "Supprimer le membre"
    override val inactivo = "Inactif"
    override val pinOpcional = "PIN (optionnel)"
    override val nuevoPinOpcional = "Nouveau PIN (optionnel)"
    override val maxSeisDig = "Max. 6 chiffres"
    override val trabajadorActivo = "Membre actif"
    override val rol = "Rôle"
    override val anadirMiembro = "Ajouter un membre à l'équipe"
    override val sinTrabajadoresPulsaPlus = "Aucun membre. Appuyez sur + pour en ajouter."
    override val emailObligatorio = "Email *"
    override val confirmarEliminarTrabajador = "Supprimer le membre"
    override val trabajadorCreado = "Membre créé"
    override val trabajadorActualizado = "Membre mis à jour"
    override val trabajadorEliminado = "Membre supprimé"

    // ── Reservas Admin ────────────────────────────────────────────────────────
    override val sinReservasEsteDia = "Aucune réservation ce jour"
    override val anadirReserva = "Ajouter une réservation"
    override val editarReserva = "Modifier la réservation"
    override val eliminarReserva = "Supprimer la réservation"
    override val nuevaReservaAdmin = "Nouvelle réservation"
    override val anadirReservaManualmente = "Ajouter une réservation manuellement"
    override val nombreCliente = "Nom *"
    override val telefonoCliente = "Téléphone *"
    override val emailOpcional = "Email (optionnel)"
    override val personas = "Personnes *"
    override val hora = "Heure *"
    override val notasOpcional = "Notes (optionnel)"
    override val diaAnterior = "Jour précédent"
    override val diaSiguiente = "Jour suivant"
    override val reservaEliminada = "Réservation supprimée"
    override val reservaCreadaCodigo = "Réservation créée · Code :"
    override val reservaActualizada = "Réservation mise à jour"
    override val confirmarEliminarReserva = "Supprimer la réservation de"
    override val errorActualizarEstado = "Erreur lors de la mise à jour du statut"

    // ── Estadísticas ──────────────────────────────────────────────────────────
    override val panelDelJefe = "Tableau de bord"
    override val sinDatosParaElPeriodo = "Aucune donnée pour la période"
    override val ventas = "Ventes"
    override val servicio = "Service"
    override val comensales = "Convives"
    override val paxTotales = "pax total"
    override val tiempoMedio = "Temps moyen"
    override val porMesa = "par table"
    override val descuentosLabel = "Remises"
    override val aplicados = "appliquées"
    override val cortesias = "Offerts"
    override val invitadas = "offerts"
    override val top3Platos = "Top 3 plats"
    override val uds = "unités"
    override val porCategoria = "Par catégorie"
    override val porMesero = "Par serveur"
    override val hoy = "Aujourd'hui"
    override val semana = "Semaine"
    override val mes = "Mois"
    override val ventaBruta = "Vente Brute"
    override val ventaNeta = "Vente Nette"
    override val delBruto = "du brut"
    override val comandas = "commandes"

    // ── Espacios Admin ────────────────────────────────────────────────────────
    override val zonasYDistribucion = "Zones et distribution du local"
    override val numeroDeEspacios = "Nombre d'espaces"
    override val hasta4Zonas = "Jusqu'à 4 zones différentes dans le local"
    override val nombresDeLosEspacios = "Noms des espaces"
    override val personalizaCadaZona = "Personnalisez chaque zone"
    override val configuracionGuardada = "Configuration enregistrée !"
    override val guardarConfiguracion = "Enregistrer la configuration"
    override val noSePuedeGuardar = "Impossible d'enregistrer"
    override val entendido = "Compris"
    override val cambiosSincronizan = "Les modifications se synchronisent sur tous les appareils du restaurant."

    // ── Plano Mesas ───────────────────────────────────────────────────────────
    override val planoDelRestaurante = "Plan du restaurant"
    override val nuevoElemento = "Nouvel élément"
    override val elementoCreado = "Élément créé"
    override val eliminado = "Supprimé"
    override val usaElBotonParaAnadir = "Utilisez le bouton + pour ajouter des éléments et des tables"
    override val tocaUnElemento = "Touchez un élément pour le sélectionner"
    override val eliminarElemento = "Supprimer l'élément"
    override val tipoElemento = "Mur, comptoir, colonne…"
    override val color = "Couleur"
    override val dimensionesYRotacion = "Dimensions et rotation"
    override val ancho = "Largeur"
    override val alto = "Hauteur"
    override val rotacion = "Rotation (°)"

    // ── Mesas Camarero ────────────────────────────────────────────────────────
    override val hola = "Bonjour,"
    override val cambiarTrabajador = "Changer de serveur"
    override val cuantosComensales = "Combien de convives ?"
    override val unoPorDefecto = "1 (par défaut)"
    override val abrirMesa = "Ouvrir la table"

    // ── Cocina ────────────────────────────────────────────────────────────────
    override val cocina = "Cuisine"
    override val todoAlDia = "Tout est à jour"
    override val salir = "Sortir"
    override val sinPedidosEnCocina = "Aucune commande en cuisine"
    override val actualizandoCada5s = "Mise à jour toutes les 5 secondes…"
    override val ahora = "Maintenant"
    override val progreso = "Progression"
    override val platosLabel = "plats"
    override val pedidoEnMarcha = "commande en cours"
    override val pedidosEnMarcha = "commandes en cours"
    override val estadoListo = "PRÊT"
    override val estadoCancelado = "ANNULÉ"
    override val estadoPreparado = "PRÉPARÉ"
    override val estadoPendiente = "EN ATTENTE"

    // ── Menú del Día Admin ────────────────────────────────────────────────────
    override val configurarOfertaHoy = "Configurer l'offre du jour"
    override val introducePrecioValido = "Entrez un prix valide"
    override val menuGuardado = "Menu enregistré ✓"
    override val guardarMenu = "Enregistrer le menu"
    override val precioDelMenu = "Prix du menu (€)"
    override val pulsaPlusParaAnadir = "Appuyez sur + pour ajouter des options"
    override val sinProductosEnCategoria = "Aucun produit dans cette catégorie"
    override val udsLabel = "Unités"
    override val seleccionaProductoYUnidades = "Sélectionnez le produit et les unités disponibles"
    override val cantidadDisponible = "Quantité disponible"
    override val dejarVacioSinLimite = "Laisser vide = sans limite"
    override val stockHint = "-1 = sans limite. 0 = épuisé. Tout autre nombre = stock disponible."
    override val noHayMasProductosCat = "Plus de produits dans cette catégorie"
    override val productoLabel = "Produit"
    override val errorAlGuardar = "Erreur lors de la sauvegarde"
    override val anadirLabel = "Ajouter"

    // ── Comanda ───────────────────────────────────────────────────────────────
    override val cancelarPedido = "Annuler la commande"
    override val enviarACocina = "Envoyer en cuisine"
    override val sinPedidoAbierto = "Aucune commande ouverte"
    override val sinProductosAun = "Aucun produit pour l'instant"
    override val enviaProductosAntesDeCobrar = "Envoyez les produits en cuisine avant d'encaisser"
    override val cobrar = "Encaisser"
    override val menuDelDiaLabel = "Menu du jour"
    override val seleccionDelDia = "Sélection du jour"
    override val agotado = "Épuisé"
    override val restantes = "restants"
    override val disponibles = "disponibles"
    override val comandarMenuDelDia = "Commander le menu du jour"
    override val modificar = "Modifier"
    override val eliminarMenu = "Supprimer le menu"
    override val quitarTodosProductosMenu = "Retirer tous les produits de ce menu de la commande ?"
    override val modificarMenu = "Modifier le menu"
    override val sinAnadir = "non ajouté"
    override val noAnadir = "Ne pas ajouter"
    override val guardarCambios = "Enregistrer les modifications"
    override val cuantosMenus = "Combien de menus ?"
    override val nDeMenus = "Nbre de menus"
    override val empezar = "Commencer"
    override val menuDelDiaAnadido = "Menu du jour ajouté"
    override val quitarDelPedido = "Retirer de la commande ?"
    override val quitar = "Retirer"
    override val mantener = "Garder"
    override val cancelarPedidoTitulo = "Annuler la commande ?"
    override val seCancelaraElPedido = "La commande sera annulée et la table sera libérée."
    override val productosSinEnviar = "Produits non envoyés"
    override val tieneProductosSinEnviar = "Vous avez des produits qui n'ont pas encore été envoyés en cuisine. Que souhaitez-vous faire ?"
    override val enviarYSalir = "Envoyer et quitter"
    override val salirSinEnviar = "Quitter sans envoyer"
    override val enviadoACocina = "Envoyé en cuisine"
    override val enviarACocinaConfirm = "Envoyer en cuisine ?"
    override val seEnviaranPendientes = "Tous les produits en attente seront envoyés en cuisine."
    override val pedidoEnviadoACocina = "Commande envoyée en cuisine"
    override val descuento = "Remise"
    override val tipoDeDescuento = "Type de remise"
    override val porcentaje = "Pourcentage (%)"
    override val fijoEur = "Fixe (€)"
    override val importeEur = "Montant (€)"
    override val quitarDescuento = "Supprimer la remise"
    override val noHayBebidasDisponibles = "Aucune boisson disponible à la carte."
    override val cobrarPedido = "Encaisser la commande"
    override val seleccionaMetodoPago = "Sélectionnez le mode de paiement"
    override val totalACobrar = "Total à encaisser"
    override val metodoDePago = "Mode de paiement"
    override val desmarcarServida = "Décocher servi"
    override val marcarServida = "Marquer servi"
    override val marcarComoServida = "Marquer comme servi"
    override val masOpciones = "Plus d'options"
    override val totalLabel = "Total"
    override val anadidoFmt = "%s ajouté"
    override val agotadoFmt = "%s épuisé"
    override val deLabel = "de"
    override val seEliminaDelPedidoFmt = "«%s» sera supprimé de la commande."
    override val menosLabel = "Moins"
    override val masLabel = "Plus"
}

val LocalStrings = compositionLocalOf<AppStrings> { EsStrings }
