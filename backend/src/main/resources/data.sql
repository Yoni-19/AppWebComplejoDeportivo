-- El rol viaja en el seed: la cuenta administradora nace como ADMIN, recepcion y operativos
-- con su rol, y el resto como CLIENTE. INSERT IGNORE no pisa las filas que ya existen, asi que
-- un cambio de rol hecho desde la aplicacion no se revierte al reiniciar. Para una base ya
-- creada hay que correr a mano:
--   UPDATE usuario SET rol = 'ADMIN' WHERE correo = 'admin@email.com';
INSERT IGNORE INTO usuario (id, nombres, apellidos, correo, clave, rol) VALUES
  (1, 'Admin', 'Sistema', 'admin@email.com', '$2a$10$QdpRmEnffZjK3RIvTCAKIOA374XrawxGxwPZBN7VsEeGhTc59fDpG', 'ADMIN'),
  (2, 'Laura', 'Gomez', 'laura@email.com', '$2a$10$QdpRmEnffZjK3RIvTCAKIOA374XrawxGxwPZBN7VsEeGhTc59fDpG', 'CLIENTE'),
  (3, 'Pedro', 'Diaz', 'pedro@email.com', '$2a$10$QdpRmEnffZjK3RIvTCAKIOA374XrawxGxwPZBN7VsEeGhTc59fDpG', 'RECEPCIONISTA'),
  (101, 'Rocio', 'Utileria', 'utileria@email.com', '$2a$10$QdpRmEnffZjK3RIvTCAKIOA374XrawxGxwPZBN7VsEeGhTc59fDpG', 'UTILERIA'),
  (102, 'Martin', 'Caja', 'caja@email.com', '$2a$10$QdpRmEnffZjK3RIvTCAKIOA374XrawxGxwPZBN7VsEeGhTc59fDpG', 'CAJA'),
  (103, 'Sergio', 'Mantenimiento', 'mantenimiento@email.com', '$2a$10$QdpRmEnffZjK3RIvTCAKIOA374XrawxGxwPZBN7VsEeGhTc59fDpG', 'MANTENIMIENTO');

-- Catalogo de implementos: no hay stock en el sistema, solo que se presta y cuanto
-- cuesta reponerlo si se pierde o se dania.
INSERT IGNORE INTO implemento (id, nombre, valor_reposicion, activo) VALUES
  (1, 'Pelota de futbol', 25.00, 1),
  (2, 'Pelota de futsal', 22.00, 1),
  (3, 'Pelota de tenis', 8.00, 1),
  (4, 'Red de voley', 45.00, 1),
  (5, 'Petos (juego de 10)', 60.00, 1);

INSERT IGNORE INTO cancha (id, nombre, deporte, precio_por_hora, activa) VALUES
  (1, 'Cancha Futbol 1', 'FUTBOL', 70.00, 1),
  (2, 'Cancha Futsal 1', 'FUTSAL', 75.00, 1),
  (3, 'Cancha Tenis 1', 'TENIS', 90.00, 1),
  (4, 'Cancha Voley 1', 'VOLEY', 60.00, 1);

-- La cancha y el horario viven en la ocupacion; la reserva solo apunta a ella.
-- La reserva cancelada del seed deja su ocupacion liberada (activa = 0).
INSERT IGNORE INTO ocupacion (id, cancha_id, fecha_hora_inicio, fecha_hora_fin, tipo, activa) VALUES
  (1, 1, '2026-11-02 15:00:00', '2026-11-02 17:00:00', 'RESERVA', 1),
  (2, 1, '2026-11-03 09:00:00', '2026-11-03 10:30:00', 'RESERVA', 1),
  (3, 3, '2026-11-04 18:00:00', '2026-11-04 19:00:00', 'RESERVA', 1),
  (4, 3, '2026-11-05 18:00:00', '2026-11-05 20:00:00', 'RESERVA', 1),
  (5, 1, '2026-11-06 11:00:00', '2026-11-06 12:00:00', 'RESERVA', 0);

INSERT IGNORE INTO reserva (id, estado, monto_total, usuario_id, ocupacion_id) VALUES
  (1, 'CONFIRMADA', 140.00, 2, 1),
  (2, 'CONFIRMADA', 140.00, 3, 2),
  (3, 'CONFIRMADA', 90.00, 2, 3),
  (4, 'COMPLETADA', 180.00, 3, 4),
  (5, 'CANCELADA', 70.00, 1, 5);

-- Detalle tipo pedido: una linea CANCHA por reserva, con el precio del momento.
-- 2 horas a 70.00 = 140.00; 1.5 horas se cobran como 2 horas iniciadas.
INSERT IGNORE INTO detalle_reserva (id, reserva_id, tipo, descripcion, cantidad, precio_unitario, subtotal) VALUES
  (1, 1, 'CANCHA', 'Cancha Futbol 1', 2, 70.00, 140.00),
  (2, 2, 'CANCHA', 'Cancha Futbol 1', 2, 70.00, 140.00),
  (3, 3, 'CANCHA', 'Cancha Tenis 1', 1, 90.00, 90.00),
  (4, 4, 'CANCHA', 'Cancha Tenis 1', 2, 90.00, 180.00),
  (5, 5, 'CANCHA', 'Cancha Futbol 1', 1, 70.00, 70.00);

-- Implementos pedidos: linea IMPLEMENTO con precio cero. Solo se cobran si se
-- pierden o se danian al devolverlos (ahi aparecen DANIO / PERDIDA).
INSERT IGNORE INTO detalle_reserva (id, reserva_id, implemento_id, tipo, descripcion, cantidad, precio_unitario, subtotal) VALUES
  (6, 1, 1, 'IMPLEMENTO', 'Pelota de futbol', 2, 0.00, 0.00),
  (7, 2, 5, 'IMPLEMENTO', 'Petos (juego de 10)', 1, 0.00, 0.00);

-- La cuenta es la cabecera contable: total, saldo pendiente y estado.
INSERT IGNORE INTO cuenta (id, reserva_id, total, saldo, estado) VALUES
  (1, 1, 140.00, 70.00, 'ABIERTA'),
  (2, 2, 140.00, 70.00, 'ABIERTA'),
  (3, 3, 90.00, 45.00, 'ABIERTA'),
  (4, 4, 180.00, 0.00, 'CERRADA'),
  (5, 5, 70.00, 0.00, 'ANULADA');

-- Movimientos: el anticipo es el 50% del total; el saldo cierra la cuenta.
INSERT IGNORE INTO pago (id, monto, tipo, metodo_pago, estado_pago, fecha_pago, cuenta_id) VALUES
  (1, 70.00, 'ANTICIPO', 'EFECTIVO', 'PAGADO', '2026-10-27 10:00:00', 1),
  (2, 70.00, 'ANTICIPO', 'TRANSFERENCIA', 'PAGADO', '2026-10-27 11:00:00', 2),
  (3, 45.00, 'ANTICIPO', 'TARJETA', 'PAGADO', '2026-10-27 12:00:00', 3),
  (4, 90.00, 'ANTICIPO', 'TARJETA', 'PAGADO', '2026-11-04 18:05:00', 4),
  (5, 90.00, 'SALDO', 'EFECTIVO', 'PAGADO', '2026-11-05 20:10:00', 4);

-- P6: entrenamiento de ejemplo. La cancha y el horario viven en la ocupacion.
INSERT IGNORE INTO ocupacion (id, cancha_id, fecha_hora_inicio, fecha_hora_fin, tipo, activa) VALUES
  (6, 2, '2026-11-20 17:00:00', '2026-11-20 18:30:00', 'ENTRENAMIENTO', 1),
  (7, 4, '2026-11-15 08:00:00', NULL, 'MANTENIMIENTO', 1);

INSERT IGNORE INTO entrenamiento (id, ocupacion_id, titulo, entrenador, descripcion, cupo_maximo, precio, estado) VALUES
  (1, 6, 'Clase de futsal', 'Diego Sosa', 'Tecnica individual y juego en equipo', 10, 30.00, 'PROGRAMADO');

INSERT IGNORE INTO inscripcion_entrenamiento (id, entrenamiento_id, usuario_id, estado, fecha_inscripcion) VALUES
  (1, 1, 2, 'INSCRITO', '2026-11-10 09:00:00');

-- P7: mantenimiento abierto de ejemplo en la cancha de voley.
INSERT IGNORE INTO mantenimiento (id, ocupacion_id, motivo, resuelto, fecha_creacion, fecha_resolucion, reservas_afectadas, reportado_por_id) VALUES
  (1, 7, 'Red de voley rota', 0, '2026-11-15 08:00:00', NULL, 0, 103);

-- La campana arranca con avisos para el admin y la recepcion.
INSERT IGNORE INTO notificacion (id, destinatario_id, tipo, titulo, mensaje, leida, fecha_creacion, referencia) VALUES
  (1, 1, 'MANTENIMIENTO_ABIERTO', 'Mantenimiento abierto', 'Se bloqueo Cancha Voley 1 por: Red de voley rota.', 0, '2026-11-15 08:00:00', 'MANTENIMIENTO:1'),
  (2, 3, 'MANTENIMIENTO_ABIERTO', 'Mantenimiento abierto', 'Se bloqueo Cancha Voley 1 por: Red de voley rota.', 0, '2026-11-15 08:00:00', 'MANTENIMIENTO:1');
