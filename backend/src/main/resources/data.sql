-- El rol viaja en el seed: la cuenta administradora nace como ADMIN y las demas como CLIENTE.
-- INSERT IGNORE no pisa las filas que ya existen, asi que un cambio de rol hecho desde la
-- aplicacion no se revierte al reiniciar. Para una base ya creada hay que correr a mano:
--   UPDATE usuario SET rol = 'ADMIN' WHERE correo = 'admin@email.com';
INSERT IGNORE INTO usuario (id, nombres, apellidos, correo, clave, rol) VALUES
  (1, 'Admin', 'Sistema', 'admin@email.com', '$2a$10$QdpRmEnffZjK3RIvTCAKIOA374XrawxGxwPZBN7VsEeGhTc59fDpG', 'ADMIN'),
  (2, 'Laura', 'Gomez', 'laura@email.com', '$2a$10$QdpRmEnffZjK3RIvTCAKIOA374XrawxGxwPZBN7VsEeGhTc59fDpG', 'CLIENTE'),
  (3, 'Pedro', 'Diaz', 'pedro@email.com', '$2a$10$QdpRmEnffZjK3RIvTCAKIOA374XrawxGxwPZBN7VsEeGhTc59fDpG', 'CLIENTE');

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

INSERT IGNORE INTO pago (id, monto, metodo_pago, estado_pago, fecha_pago, reserva_id) VALUES
  (4, 140.00, 'EFECTIVO', 'PAGADO', '2026-11-02 17:30:00', 1),
  (5, 90.00, 'TRANSFERENCIA', 'PENDIENTE', '2026-11-04 19:15:00', 3),
  (6, 180.00, 'TARJETA', 'PAGADO', '2026-11-05 20:30:00', 4);
