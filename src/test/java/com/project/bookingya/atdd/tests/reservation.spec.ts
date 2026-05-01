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

import { test, expect, APIRequestContext } from '@playwright/test';

const base = 'http://localhost:8080/api';

// INTERFACES

interface ReservationResponse {
  id:       string;
  guestId?: string;
  roomId?:  string;
  guest?:   { id: string };
  room?:    { id: string };
  checkIn:  string;
  checkOut: string;
  notes:    string;
}

// ────────────────────────────────────────────────────────────
// CONSTANTES (DATOS DE PRUEBA CONTROLADOS)
// Centralizar fechas evita duplicación y facilita mantenimiento
// ────────────────────────────────────────────────────────────

const FECHAS = {
  checkIn:             '2026-06-01T00:00:00',
  checkOut:            '2026-06-05T00:00:00',
  checkInActualizado:  '2026-06-02T00:00:00',
  checkOutActualizado: '2026-06-06T00:00:00',
};

// ────────────────────────────────────────────────────────────
// FACTORY DE RESERVAS
// Genera el payload base para evitar repetición
// Facilita cambios futuros en la estructura del request
// ────────────────────────────────────────────────────────────

const RESERVA_BASE = (guestId: string, roomId: string, notes: string) => ({
  guestId,
  roomId,
  checkIn:     FECHAS.checkIn,
  checkOut:    FECHAS.checkOut,
  guestsCount: 2,
  notes
});

// ──────────────────────────────────────────────────────────────
// HELPER: CREACIÓN DE DATOS BASE
// Crea huésped + habitación únicos por ejecución
// ──────────────────────────────────────────────────────────────

async function crearGuestYRoom(request: APIRequestContext) {
  const unique = Date.now();

  const guestRes = await request.post(`${base}/guest`, {
    data: {
      identification: unique.toString(),
      name:  `User ${unique}`,
      email: `user${unique}@test.com`
    }
  });
  const guestId = (await guestRes.json()).id;

  const roomRes = await request.post(`${base}/room`, {
    data: {
      code:         `ROOM-${unique}`,
      name:         'Room ATDD',
      city:         'Medellin',
      maxGuests:    2,
      nightlyPrice: 100000,
      available:    true
    }
  });
  const roomId = (await roomRes.json()).id;

  return { guestId, roomId };
}

// ──────────────────────────────────────────────────────────────
// HELPER: CREAR RESERVA
// Encapsula la creación y validación básica
// ──────────────────────────────────────────────────────────────

async function crearReserva(
  request: APIRequestContext,
  guestId: string,
  roomId:  string,
  notes:   string
): Promise<string> {
  const res = await request.post(`${base}/reservation`, {
    data: RESERVA_BASE(guestId, roomId, notes)
  });
  expect(res.status()).toBe(200);
  const body = await res.json();
  expect(body.id).toBeTruthy();
  return body.id;
}

// ────────────────────────────────────────────────────────────
// SUITE DE PRUEBAS ATDD
// Cada test valida un criterio de aceptación completo
// ────────────────────────────────────────────────────────────

test.describe('ATDD - Gestión de Reservas', () => {

  let guestId: string;
  let roomId:  string;

  test.beforeEach(async ({ request }) => {
    const context = await crearGuestYRoom(request);
    guestId = context.guestId;
    roomId  = context.roomId;
  });

  // ───────────────────────────────────────────────────────────
  // ESCENARIO 1: CREACIÓN DE UNA RESERVA
  // Valida persistencia completa de datos
  // ───────────────────────────────────────────────────────────

  test('Creación de una reserva', async ({ request }) => {
    const res = await request.post(`${base}/reservation`, {
      data: RESERVA_BASE(guestId, roomId, 'ATDD create')
    });

    expect(res.status()).toBe(200);
    const body: ReservationResponse = await res.json();
    expect(body.id).toBeTruthy();
    expect(body.checkIn).toContain('2026-06-01');
    expect(body.checkOut).toContain('2026-06-05');
    expect(body.guestId ?? body.guest?.id).toBe(guestId);
    expect(body.roomId  ?? body.room?.id).toBe(roomId);
  });

  // ───────────────────────────────────────────────────────────
  // ESCENARIO 2: OBTENCIÓN DE UNA RESERVA POR ID
  // Valida consistencia de datos recuperados
  // ───────────────────────────────────────────────────────────

  test('Obtención de una reserva por ID', async ({ request }) => {
    const reservationId = await crearReserva(request, guestId, roomId, 'ATDD getById');

    const res = await request.get(`${base}/reservation/${reservationId}`);
    expect(res.status()).toBe(200);

    const body: ReservationResponse = await res.json();
    expect(body.id).toBe(reservationId);
    expect(body.checkIn).toContain('2026-06-01');
    expect(body.checkOut).toContain('2026-06-05');
  });

  // ───────────────────────────────────────────────────────────
  // ESCENARIO 3: CONSULTA DE RESERVAS POR HUÉSPED
  // Valida integridad de colección de datos
  // ───────────────────────────────────────────────────────────

  test('Consulta de una reserva', async ({ request }) => {
    await crearReserva(request, guestId, roomId, 'ATDD list');

    const res = await request.get(`${base}/reservation/guest/${guestId}`);
    expect(res.status()).toBe(200);

    const body: ReservationResponse[] = await res.json();
    expect(Array.isArray(body)).toBe(true);
    expect(body.length).toBeGreaterThan(0);
    body.forEach(r => {
      expect(r.guestId ?? r.guest?.id).toBe(guestId);
    });
  });

  // ───────────────────────────────────────────────────────────
  // ESCENARIO 4: ACTUALIZACIÓN DE UNA RESERVA EXISTENTE
  // Valida persistencia de cambios
  // ───────────────────────────────────────────────────────────

  test('Actualización de una reserva existente', async ({ request }) => {
    const reservationId = await crearReserva(request, guestId, roomId, 'ATDD update');

    const res = await request.put(`${base}/reservation/${reservationId}`, {
      data: {
        guestId,
        roomId,
        checkIn:     FECHAS.checkInActualizado,
        checkOut:    FECHAS.checkOutActualizado,
        guestsCount: 2,
        notes:       'UPDATED'
      }
    });

    expect(res.status()).toBe(200);
    const body: ReservationResponse = await res.json();
    expect(body.checkIn).toContain('2026-06-02');
    expect(body.checkOut).toContain('2026-06-06');
    expect(body.notes).toBe('UPDATED');
  });

  // ───────────────────────────────────────────────────────────
  // ESCENARIO 5: ELIMINACIÓN DE UNA RESERVA
  // Valida eliminación efectiva (no solo respuesta)
  // ───────────────────────────────────────────────────────────

  test('Eliminación de una reserva', async ({ request }) => {
    const reservationId = await crearReserva(request, guestId, roomId, 'ATDD delete');

    const del = await request.delete(`${base}/reservation/${reservationId}`);
    expect(del.status()).toBe(200);

    const verify = await request.get(`${base}/reservation/${reservationId}`);
    expect(verify.status()).toBe(404);
  });

});