// ============================================================
// TDD - PRUEBAS UNITARIAS DEL SERVICIO DE RESERVAS
// ------------------------------------------------------------
// Propósito:
// Validar la lógica de negocio del servicio ReservationService
// de forma aislada utilizando mocks.
//
// Enfoque:
// - Se utiliza JUnit 5 para pruebas unitarias
// - Se utiliza Mockito para simular dependencias
// - Se validan reglas de negocio internas del servicio
//
// Diferencia con otros enfoques:
// - BDD → comportamiento del negocio (Gherkin)
// - ATDD → validación end-to-end (API real)
// - TDD → validación de lógica interna (unitaria)
//
// Flujo:
// Test → Service → Mocks → Validación
// ============================================================

package com.project.bookingya.tdd;

import com.project.bookingya.dtos.ReservationDto;
import com.project.bookingya.entities.ReservationEntity;
import com.project.bookingya.entities.RoomEntity;
import com.project.bookingya.models.Reservation;
import com.project.bookingya.repositories.IGuestRepository;
import com.project.bookingya.repositories.IReservationRepository;
import com.project.bookingya.repositories.IRoomRepository;
import com.project.bookingya.services.ReservationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ReservationServiceTest {

    // ─────────────────────────────────────────
    // DEPENDENCIAS MOCKEADAS
    // ─────────────────────────────────────────

    private IReservationRepository reservationRepository;
    private IRoomRepository roomRepository;
    private IGuestRepository guestRepository;
    private ModelMapper modelMapper;
    private ReservationService service;

    // ─────────────────────────────────────────
    // CONFIGURACIÓN INICIAL
    // Se ejecuta antes de cada prueba
    // ─────────────────────────────────────────

    @BeforeEach
    void setup() {
        reservationRepository = mock(IReservationRepository.class);
        roomRepository        = mock(IRoomRepository.class);
        guestRepository       = mock(IGuestRepository.class);
        modelMapper           = mock(ModelMapper.class);

        service = new ReservationService(
                reservationRepository,
                roomRepository,
                guestRepository,
                modelMapper
        );
    }

    // ─────────────────────────────────────────
    // ESCENARIO 1: CREACIÓN DE UNA RESERVA
    // Valida que el servicio permita crear una
    // reserva válida cumpliendo reglas de negocio:
    // - fechas válidas
    // - huésped existente
    // - habitación disponible
    // - sin solapamiento de fechas
    // ─────────────────────────────────────────

    @Test
    void shouldCreateReservationSuccessfully() {

        ReservationDto dto = new ReservationDto();
        dto.setRoomId(UUID.randomUUID());
        dto.setGuestId(UUID.randomUUID());
        dto.setCheckIn(LocalDateTime.now().plusDays(1));
        dto.setCheckOut(LocalDateTime.now().plusDays(3));
        dto.setGuestsCount(2);

        RoomEntity room = new RoomEntity();
        room.setAvailable(true);
        room.setMaxGuests(4);

        ReservationEntity entity   = new ReservationEntity();
        Reservation       expected = new Reservation();

        when(roomRepository.findById(any()))
                .thenReturn(Optional.of(room));

        when(guestRepository.findById(any()))
                .thenReturn(Optional.of(mock()));

        when(reservationRepository.existsOverlappingReservationForRoom(
                any(), any(), any(), any()))
                .thenReturn(false);

        when(reservationRepository.existsOverlappingReservationForGuest(
                any(), any(), any(), any()))
                .thenReturn(false);

        when(modelMapper.map(dto, ReservationEntity.class))
                .thenReturn(entity);

        when(reservationRepository.saveAndFlush(any(ReservationEntity.class)))
                .thenReturn(entity);

        when(modelMapper.map(entity, Reservation.class))
                .thenReturn(expected);

        Reservation result = service.create(dto);

        assertNotNull(result);
        verify(reservationRepository).saveAndFlush(any(ReservationEntity.class));
    }

    // ─────────────────────────────────────────
    // ESCENARIO 2: OBTENCIÓN DE UNA RESERVA POR ID
    // Valida que el servicio retorne correctamente
    // una reserva existente dado su identificador
    // ─────────────────────────────────────────

    @Test
    void shouldReturnReservationById() {

        UUID              id       = UUID.randomUUID();
        ReservationEntity entity   = new ReservationEntity();
        Reservation       expected = new Reservation();
        expected.setId(id);

        when(reservationRepository.findById(id))
                .thenReturn(Optional.of(entity));

        when(modelMapper.map(entity, Reservation.class))
                .thenReturn(expected);

        Reservation result = service.getById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(reservationRepository).findById(id);
    }

    // ─────────────────────────────────────────
    // ESCENARIO 3: CONSULTA DE RESERVAS POR HUÉSPED
    // Valida que el servicio retorne todas las
    // reservas asociadas a un huésped específico
    // ─────────────────────────────────────────
    
    @Test
    void shouldReturnReservationsByGuestId() {

        UUID guestId = UUID.randomUUID();

        ReservationEntity entity1 = new ReservationEntity();
        ReservationEntity entity2 = new ReservationEntity();

        List<ReservationEntity> entities     = List.of(entity1, entity2);
        List<Reservation>       reservations = List.of(new Reservation(), new Reservation());

        when(reservationRepository.findByGuestId(guestId))
                .thenReturn(entities);

        when(modelMapper.map(eq(entities), any(Type.class)))
                .thenReturn(reservations);

        List<Reservation> result = service.getByGuestId(guestId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(reservationRepository).findByGuestId(guestId);
    }

    // ─────────────────────────────────────────
    // ESCENARIO 4: ACTUALIZACIÓN DE UNA RESERVA
    // Valida que el servicio actualice correctamente
    // una reserva existente cumpliendo las
    // validaciones de negocio
    // ─────────────────────────────────────────

    @Test
    void shouldUpdateReservationSuccessfully() {

        UUID id = UUID.randomUUID();

        ReservationDto dto = new ReservationDto();
        dto.setRoomId(UUID.randomUUID());
        dto.setGuestId(UUID.randomUUID());
        dto.setCheckIn(LocalDateTime.now().plusDays(1));
        dto.setCheckOut(LocalDateTime.now().plusDays(3));
        dto.setGuestsCount(2);

        RoomEntity room = new RoomEntity();
        room.setAvailable(true);
        room.setMaxGuests(4);

        ReservationEntity entity   = new ReservationEntity();
        entity.setId(id);
        Reservation       expected = new Reservation();
        expected.setId(id);

        when(reservationRepository.findById(id))
                .thenReturn(Optional.of(entity));

        when(roomRepository.findById(any()))
                .thenReturn(Optional.of(room));

        when(guestRepository.findById(any()))
                .thenReturn(Optional.of(mock()));

        when(reservationRepository.existsOverlappingReservationForRoom(
                any(), any(), any(), any()))
                .thenReturn(false);

        when(reservationRepository.existsOverlappingReservationForGuest(
                any(), any(), any(), any()))
                .thenReturn(false);

        when(reservationRepository.saveAndFlush(any(ReservationEntity.class)))
                .thenReturn(entity);

        when(modelMapper.map(entity, Reservation.class))
                .thenReturn(expected);

        Reservation result = service.update(dto, id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(reservationRepository).saveAndFlush(any(ReservationEntity.class));
    }

    // ─────────────────────────────────────────
    // ESCENARIO 5: ELIMINACIÓN DE UNA RESERVA
    // Valida que el servicio elimine correctamente
    // una reserva existente dado su identificador
    // ─────────────────────────────────────────

    @Test
    void shouldDeleteReservationSuccessfully() {

        UUID              id     = UUID.randomUUID();
        ReservationEntity entity = new ReservationEntity();
        entity.setId(id);

        when(reservationRepository.findById(id))
                .thenReturn(Optional.of(entity));

        doNothing().when(reservationRepository).delete(entity);
        doNothing().when(reservationRepository).flush();

        service.delete(id);

        verify(reservationRepository).delete(entity);
        verify(reservationRepository).flush();
    }
}