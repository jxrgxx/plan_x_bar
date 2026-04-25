-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: pdb1037.awardspace.net
-- Tiempo de generación: 25-04-2026 a las 17:20:53
-- Versión del servidor: 8.0.32
-- Versión de PHP: 8.1.34

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `4742077_planbar`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `Mesas`
--

CREATE TABLE `Mesas` (
  `id` int NOT NULL,
  `restaurante_id` int NOT NULL,
  `codigo` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `capacidad` int NOT NULL,
  `estado` enum('libre','ocupada','reservada') COLLATE utf8mb4_unicode_ci DEFAULT 'libre',
  `posX` float DEFAULT '0',
  `posY` float DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `PedidoProductos`
--

CREATE TABLE `PedidoProductos` (
  `id` int NOT NULL,
  `pedido_id` int NOT NULL,
  `producto_id` int NOT NULL,
  `cantidad` int NOT NULL DEFAULT '1',
  `precio_unitario` decimal(10,2) NOT NULL,
  `observaciones` text COLLATE utf8mb4_unicode_ci,
  `fecha_agregado` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `Pedidos`
--

CREATE TABLE `Pedidos` (
  `id` int NOT NULL,
  `restaurante_id` int NOT NULL,
  `mesa_id` int NOT NULL,
  `reserva_id` int DEFAULT NULL,
  `trabajador_id` int DEFAULT NULL,
  `fecha_apertura` datetime DEFAULT CURRENT_TIMESTAMP,
  `fecha_cierre` datetime DEFAULT NULL,
  `estado` enum('abierto','pagado') COLLATE utf8mb4_unicode_ci DEFAULT 'abierto',
  `subtotal` decimal(10,2) DEFAULT '0.00',
  `total` decimal(10,2) DEFAULT '0.00',
  `metodo_pago` enum('efectivo','tarjeta','otro') COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `Productos`
--

CREATE TABLE `Productos` (
  `id` int NOT NULL,
  `restaurante_id` int NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `categoria` enum('entrante','principal','postre','bebida') COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `precio` decimal(10,2) NOT NULL,
  `disponible` tinyint(1) DEFAULT '1',
  `imagen_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_creacion` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `ReservaMesas`
--

CREATE TABLE `ReservaMesas` (
  `id` int NOT NULL,
  `reserva_id` int NOT NULL,
  `mesa_id` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `Reservas`
--

CREATE TABLE `Reservas` (
  `id` int NOT NULL,
  `codigo` varchar(6) COLLATE utf8mb4_unicode_ci NOT NULL,
  `restaurante_id` int NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `telefono` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `correo` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `num_personas` int NOT NULL,
  `fecha` date NOT NULL,
  `hora` time NOT NULL,
  `estado` enum('pendiente','confirmada','cancelada','completada') COLLATE utf8mb4_unicode_ci DEFAULT 'pendiente',
  `notas` text COLLATE utf8mb4_unicode_ci,
  `fecha_creacion` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `Restaurante`
--

CREATE TABLE `Restaurante` (
  `id` int NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `direccion` text COLLATE utf8mb4_unicode_ci,
  `telefono` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_creacion` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `Trabajadores`
--

CREATE TABLE `Trabajadores` (
  `id` int NOT NULL,
  `restaurante_id` int NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rol` enum('admin','camarero','cocina') COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `activo` tinyint(1) DEFAULT '1',
  `fecha_creacion` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `Mesas`
--
ALTER TABLE `Mesas`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_restaurante_codigo` (`restaurante_id`,`codigo`),
  ADD KEY `idx_estado` (`estado`);

--
-- Indices de la tabla `PedidoProductos`
--
ALTER TABLE `PedidoProductos`
  ADD PRIMARY KEY (`id`),
  ADD KEY `producto_id` (`producto_id`),
  ADD KEY `idx_pedido` (`pedido_id`);

--
-- Indices de la tabla `Pedidos`
--
ALTER TABLE `Pedidos`
  ADD PRIMARY KEY (`id`),
  ADD KEY `restaurante_id` (`restaurante_id`),
  ADD KEY `reserva_id` (`reserva_id`),
  ADD KEY `trabajador_id` (`trabajador_id`),
  ADD KEY `idx_mesa_estado` (`mesa_id`,`estado`),
  ADD KEY `idx_fecha` (`fecha_apertura`),
  ADD KEY `idx_estado` (`estado`);

--
-- Indices de la tabla `Productos`
--
ALTER TABLE `Productos`
  ADD PRIMARY KEY (`id`),
  ADD KEY `restaurante_id` (`restaurante_id`),
  ADD KEY `idx_categoria` (`categoria`),
  ADD KEY `idx_disponible` (`disponible`);

--
-- Indices de la tabla `ReservaMesas`
--
ALTER TABLE `ReservaMesas`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_reserva_mesa` (`reserva_id`,`mesa_id`),
  ADD KEY `mesa_id` (`mesa_id`);

--
-- Indices de la tabla `Reservas`
--
ALTER TABLE `Reservas`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `codigo` (`codigo`),
  ADD KEY `restaurante_id` (`restaurante_id`),
  ADD KEY `idx_codigo` (`codigo`),
  ADD KEY `idx_fecha_hora` (`fecha`,`hora`),
  ADD KEY `idx_estado` (`estado`);

--
-- Indices de la tabla `Restaurante`
--
ALTER TABLE `Restaurante`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indices de la tabla `Trabajadores`
--
ALTER TABLE `Trabajadores`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `idx_restaurante` (`restaurante_id`),
  ADD KEY `idx_email` (`email`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `Mesas`
--
ALTER TABLE `Mesas`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `PedidoProductos`
--
ALTER TABLE `PedidoProductos`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `Pedidos`
--
ALTER TABLE `Pedidos`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `Productos`
--
ALTER TABLE `Productos`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `ReservaMesas`
--
ALTER TABLE `ReservaMesas`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `Reservas`
--
ALTER TABLE `Reservas`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `Restaurante`
--
ALTER TABLE `Restaurante`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `Trabajadores`
--
ALTER TABLE `Trabajadores`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `Mesas`
--
ALTER TABLE `Mesas`
  ADD CONSTRAINT `Mesas_ibfk_1` FOREIGN KEY (`restaurante_id`) REFERENCES `Restaurante` (`id`) ON DELETE CASCADE;

--
-- Filtros para la tabla `PedidoProductos`
--
ALTER TABLE `PedidoProductos`
  ADD CONSTRAINT `PedidoProductos_ibfk_1` FOREIGN KEY (`pedido_id`) REFERENCES `Pedidos` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `PedidoProductos_ibfk_2` FOREIGN KEY (`producto_id`) REFERENCES `Productos` (`id`) ON DELETE RESTRICT;

--
-- Filtros para la tabla `Pedidos`
--
ALTER TABLE `Pedidos`
  ADD CONSTRAINT `Pedidos_ibfk_1` FOREIGN KEY (`restaurante_id`) REFERENCES `Restaurante` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `Pedidos_ibfk_2` FOREIGN KEY (`mesa_id`) REFERENCES `Mesas` (`id`) ON DELETE RESTRICT,
  ADD CONSTRAINT `Pedidos_ibfk_3` FOREIGN KEY (`reserva_id`) REFERENCES `Reservas` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `Pedidos_ibfk_4` FOREIGN KEY (`trabajador_id`) REFERENCES `Trabajadores` (`id`) ON DELETE SET NULL;

--
-- Filtros para la tabla `Productos`
--
ALTER TABLE `Productos`
  ADD CONSTRAINT `Productos_ibfk_1` FOREIGN KEY (`restaurante_id`) REFERENCES `Restaurante` (`id`) ON DELETE CASCADE;

--
-- Filtros para la tabla `ReservaMesas`
--
ALTER TABLE `ReservaMesas`
  ADD CONSTRAINT `ReservaMesas_ibfk_1` FOREIGN KEY (`reserva_id`) REFERENCES `Reservas` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `ReservaMesas_ibfk_2` FOREIGN KEY (`mesa_id`) REFERENCES `Mesas` (`id`) ON DELETE CASCADE;

--
-- Filtros para la tabla `Reservas`
--
ALTER TABLE `Reservas`
  ADD CONSTRAINT `Reservas_ibfk_1` FOREIGN KEY (`restaurante_id`) REFERENCES `Restaurante` (`id`) ON DELETE CASCADE;

--
-- Filtros para la tabla `Trabajadores`
--
ALTER TABLE `Trabajadores`
  ADD CONSTRAINT `Trabajadores_ibfk_1` FOREIGN KEY (`restaurante_id`) REFERENCES `Restaurante` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
