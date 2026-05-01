// ────────────────────────────────────────────────────────────
// ATDD - PLAYWRIGHT TESTS
// ────────────────────────────────────────────────────────────
// Objetivo:
// Validar el comportamiento del sistema desde la perspectiva
// del usuario final.
//
// Enfoque:
// - Se utiliza Playwright para ejecutar pruebas end-to-end
// - Se validan flujos completos (no solo endpoints aislados)
// - Se manejan datos dinámicos para evitar conflictos
//
// Diferencia con BDD:
// - BDD → comportamiento del negocio (Gherkin)
// - ATDD → validación real del sistema (usuario/API)
//
// ────────────────────────────────────────────────────────────

import { test, expect } from '@playwright/test';

const base = 'http://localhost:8080/api';

test.describe('ATDD - Gestión de Reservas', () => {

  // MÉTODO:
  // Crea un huésped y una habitación para ser usados en los tests
  // Evita duplicidad de código y mejora la mantenibilidad
  async function crearGuestYRoom(request: any) {
    const unique = Date.now();

    // CREAR HUÉSPED
    const guestRes = await request.post(`${base}/guest`, {
      data: {
        identification: unique.toString(),
        name: `User ${unique}`,
        email: `user${unique}@test.com`
      }
    });

    const guestId = (await guestRes.json()).id;

    // CREAR HABITACIÓN
    const roomRes = await request.post(`${base}/room`, {
      data: {
        code: `ROOM-${unique}`,
        name: 'Room ATDD',
        city: 'Medellin',
        maxGuests: 2,
        nightlyPrice: 100000,
        available: true
      }
    });

    const roomId = (await roomRes.json()).id;

    return { guestId, roomId };
  }

  // ────────────────────────────────────────────────────────────────────
  // ESCENARIO 1: CREACIÓN DE UNA RESERVA
  // Valida que el sistema permita crear una reserva con datos válidos
  // ────────────────────────────────────────────────────────────────────

  test('Creación de una reserva', async ({ request }) => {

    const { guestId, roomId } = await crearGuestYRoom(request);

    const res = await request.post(`${base}/reservation`, {
      data: {
        guestId,
        roomId,
        checkIn: '2026-06-01T00:00:00',
        checkOut: '2026-06-05T00:00:00',
        guestsCount: 2,
        notes: 'ATDD create'
      }
    });

    // Validación del estado exitoso
    expect(res.status()).toBe(200);

    const body = await res.json();

    // El sistema asigna un ID a la reserva creada
    expect(body.id).toBeTruthy();

    // Las fechas persisten correctamente
    expect(body.checkIn).toContain('2026-06-01');
    expect(body.checkOut).toContain('2026-06-05');

    // Los datos del huesped y habitacion son correctos
    expect(body.guestId ?? body.guest?.id).toBe(guestId);
    expect(body.roomId ?? body.room?.id).toBe(roomId);
  });

  // ────────────────────────────────────────────────────────────────────
  // ESCENARIO 2: OBTENCIÓN DE UNA RESERVA POR ID
  // Valida que el sistema retorne correctamente una reserva específica
  // ────────────────────────────────────────────────────────────────────

  test('Obtención de una reserva por ID', async ({ request }) => {

    const { guestId, roomId } = await crearGuestYRoom(request);

    const create = await request.post(`${base}/reservation`, {
      data: {
        guestId,
        roomId,
        checkIn: '2026-06-01T00:00:00',
        checkOut: '2026-06-05T00:00:00',
        guestsCount: 2,
        notes: 'ATDD getById'
      }
    });

    const reservationId = (await create.json()).id;

    const res = await request.get(`${base}/reservation/${reservationId}`);

    // Validación del estado exitoso
    expect(res.status()).toBe(200);

    const body = await res.json();

    // El ID retornado coincide con el consultado
    expect(body.id).toBe(reservationId);

    // Los datos son consistentes con la creacion
    expect(body.checkIn).toContain('2026-06-01');
    expect(body.checkOut).toContain('2026-06-05');
  });

  // ────────────────────────────────────────────────────────────────────
  // ESCENARIO 3: CONSULTA DE UNA RESERVA POR HUÉSPED
  // Validar que el sistema retorne todas las reservas asociadas a un huésped específico
  // ────────────────────────────────────────────────────────────────────

  test('Consulta de una reserva', async ({ request }) => {

    const { guestId, roomId } = await crearGuestYRoom(request);

    await request.post(`${base}/reservation`, {
      data: {
        guestId,
        roomId,
        checkIn: '2026-06-01T00:00:00',
        checkOut: '2026-06-05T00:00:00',
        guestsCount: 2,
        notes: 'ATDD list'
      }
    });

    const res = await request.get(`${base}/reservation/guest/${guestId}`);

    // Validación del estado exitoso
    expect(res.status()).toBe(200);

    const body = await res.json();

    // La respuesta es una lista
    expect(Array.isArray(body)).toBe(true);

    // La lista contiene al menos una reserva
    expect(body.length).toBeGreaterThan(0);

    // Todas las reservas pertenecen al huesped consultado
    body.forEach((reservation: any) => {
      const idEnRespuesta = reservation.guestId ?? reservation.guest?.id;
      expect(idEnRespuesta).toBe(guestId);
    });
  });

  // ────────────────────────────────────────────────────────────────────
  // ESCENARIO 4: ACTUALIZACIÓN DE UNA RESERVA EXISTENTE
  // Valida que el sistema permita modificar una reserva existente
  // ────────────────────────────────────────────────────────────────────

  test('Actualización de una reserva existente', async ({ request }) => {

    const { guestId, roomId } = await crearGuestYRoom(request);

    const create = await request.post(`${base}/reservation`, {
      data: {
        guestId,
        roomId,
        checkIn: '2026-06-01T00:00:00',
        checkOut: '2026-06-05T00:00:00',
        guestsCount: 2,
        notes: 'ATDD update'
      }
    });

    const reservationId = (await create.json()).id;

    const res = await request.put(`${base}/reservation/${reservationId}`, {
      data: {
        guestId,
        roomId,
        checkIn: '2026-06-02T00:00:00',
        checkOut: '2026-06-06T00:00:00',
        guestsCount: 2,
        notes: 'UPDATED'
      }
    });

    // Validación del estado exitoso
    expect(res.status()).toBe(200);

    const body = await res.json();

    // Las nuevas fechas quedaron persistidas
    expect(body.checkIn).toContain('2026-06-02');
    expect(body.checkOut).toContain('2026-06-06');

    // Las notas actualizadas se reflejan
    expect(body.notes).toBe('UPDATED');
  });

  // ────────────────────────────────────────────────────────────────────
  // ESCENARIO 5: ELIMINACIÓN DE UNA RESERVA
  // Valida que el sistema permita eliminar una reserva
  // ────────────────────────────────────────────────────────────────────

  test('Eliminación de una reserva', async ({ request }) => {

    const { guestId, roomId } = await crearGuestYRoom(request);

    const create = await request.post(`${base}/reservation`, {
      data: {
        guestId,
        roomId,
        checkIn: '2026-06-01T00:00:00',
        checkOut: '2026-06-05T00:00:00',
        guestsCount: 2,
        notes: 'ATDD delete'
      }
    });

    const reservationId = (await create.json()).id;
    const res = await request.delete(`${base}/reservation/${reservationId}`);

    // Validación del estado exitoso
    expect(res.status()).toBe(200);

    // Verificar que la reserva ya no existe en el sistema
    const verify = await request.get(`${base}/reservation/${reservationId}`);
    expect(verify.status()).toBe(404);
  });

});