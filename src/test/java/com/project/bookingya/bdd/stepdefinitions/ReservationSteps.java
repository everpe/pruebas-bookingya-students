// ─────────────────────────────────────────────────────────
// BDD - STEP DEFINITIONS: ReservationSteps
// ─────────────────────────────────────────────────────────

// Objetivo:
// Implementar los pasos de Cucumber que conectan los escenarios
// Gherkin (.feature) con llamadas HTTP directas usando RestAssured.
//
// Enfoque:
// - Cada paso (@Given/@When/@Then) llama directamente a la API
//   usando RestAssured
// - El contexto entre pasos se comparte mediante variables de
//   instancia de la clase (guestId, roomId, reservationId, response)
//
// Flujo:
// Gherkin → StepDefinition → RestAssured → API REST
// ─────────────────────────────────────────────────────────

package com.project.bookingya.bdd.stepdefinitions;

import io.cucumber.java.en.*;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ReservationSteps {

    // ─────────────────────────────────────────────────────────
    // CONTEXTO COMPARTIDO ENTRE PASOS
    // Variables de instancia que persisten dentro del escenario
    // ─────────────────────────────────────────────────────────

    private static final String BASE_URL = "http://localhost:8080/api";

    private String guestId;
    private String roomId;
    private String reservationId;
    private Response lastResponse;

    // ─────────────────────────────────────────────────────────
    // CONFIGURACIÓN BASE
    // ─────────────────────────────────────────────────────────
    private void configBase() {
        baseURI = BASE_URL;
    }

    // ─────────────────────────────────────────
    // FACTORY JSON
    // ─────────────────────────────────────────
    private Map<String, Object> guestPayload(long unique) {
        Map<String, Object> data = new HashMap<>();
        data.put("identification", "QA-" + unique);
        data.put("name", "QA User");
        data.put("email", "qa" + unique + "@bookingya.com");
        return data;
    }

    private Map<String, Object> roomPayload(long unique) {
        Map<String, Object> data = new HashMap<>();
        data.put("code", "HAB-" + unique);
        data.put("name", "Habitacion QA BDD");
        data.put("city", "Medellin");
        data.put("maxGuests", 3);
        data.put("nightlyPrice", 150000);
        data.put("available", true);
        return data;
    }

    private Map<String, Object> reservationPayload() {
        Map<String, Object> data = new HashMap<>();
        data.put("guestId", guestId);
        data.put("roomId", roomId);
        data.put("checkIn", "2026-07-01T14:00:00");
        data.put("checkOut", "2026-07-05T12:00:00");
        data.put("guestsCount", 2);
        data.put("notes", "Reserva BDD");
        return data;
    }

    // ─────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────

    private void crearGuestYRoom() {
        configBase();
        long unique = System.currentTimeMillis();

        // Crear guest
        lastResponse = given()
                .contentType("application/json")
                .body(guestPayload(unique))
                .post("/guest");

        guestId = lastResponse.jsonPath().getString("id");
        validarCreacion("guest", guestId);

        // Crear room
        lastResponse = given()
                .contentType("application/json")
                .body(roomPayload(unique))
                .post("/room");

        roomId = lastResponse.jsonPath().getString("id");
        validarCreacion("room", roomId);
    }

    private void crearReservaBase() {
        lastResponse = given()
                .contentType("application/json")
                .body(reservationPayload())
                .post("/reservation");

        reservationId = lastResponse.jsonPath().getString("id");
        validarCreacion("reserva", reservationId);
    }

    private void validarCreacion(String entidad, String id) {
        if (id == null) {
            throw new RuntimeException(
                    "Error creando " + entidad +
                            ". Status: " + lastResponse.statusCode() +
                            " Body: " + lastResponse.asString()
            );
        }
    }
// ─────────────────────────────────────────────────────────
// GIVEN: Huésped y habitación
// ─────────────────────────────────────────────────────────

    @Given("existe un huésped y una habitación disponible")
    public void setupBase() {
        crearGuestYRoom();
    }

// ─────────────────────────────────────────────────────────
// GIVEN: RESERVA EXISTENTE
// ─────────────────────────────────────────────────────────

    @Given("existe una reserva creada")
    public void setupReserva() {
        crearGuestYRoom();
        crearReservaBase();
    }

// ─────────────────────────────────────────────────────────
// ESCENARIO 1: CREACIÓN DE UNA RESERVA
// ─────────────────────────────────────────────────────────

    @When("creo una reserva válida")
    public void crearReserva() {
        crearReservaBase();
    }

    @Then("la reserva se crea exitosamente")
    public void validarCreacionReserva() {
        assertThat(lastResponse.statusCode(), equalTo(200));
        assertThat(lastResponse.jsonPath().getString("id"), notNullValue());
    }

// ─────────────────────────────────────────────────────────
// ESCENARIO 2: OBTENCIÓN DE UNA RESERVA POR ID
// ─────────────────────────────────────────────────────────

    @When("consulto la reserva por su ID")
    public void getById() {
        lastResponse = given().get("/reservation/" + reservationId);
    }

    @Then("debo obtener la reserva por ID correctamente")
    public void validarGet() {
        assertThat(lastResponse.statusCode(), equalTo(200));
    }

// ─────────────────────────────────────────────────────────
// ESCENARIO 3: CONSULTA DE RESERVAS POR HUÉSPED
// ─────────────────────────────────────────────────────────

    @When("consulto las reservas del huésped")
    public void getByGuest() {
        lastResponse = given().get("/reservation/guest/" + guestId);
    }

    @Then("debo obtener las reservas del huésped")
    public void validarLista() {
        assertThat(lastResponse.statusCode(), equalTo(200));
    }

// ─────────────────────────────────────────────────────────
// ESCENARIO 4: ACTUALIZACIÓN DE UNA RESERVA EXISTENTE
// ─────────────────────────────────────────────────────────

    @When("actualizo la reserva")
    public void updateReserva() {

        Map<String, Object> update = new HashMap<>();
        update.put("guestId", guestId);
        update.put("roomId", roomId);
        update.put("checkIn", "2026-08-10T14:00:00");
        update.put("checkOut", "2026-08-15T12:00:00");
        update.put("guestsCount", 1);
        update.put("notes", "Reserva actualizada");

        lastResponse = given()
                .contentType("application/json")
                .body(update)
                .put("/reservation/" + reservationId);
    }

    @Then("la reserva debe actualizarse correctamente")
    public void validarUpdate() {
        assertThat(lastResponse.statusCode(), equalTo(200));
    }

// ─────────────────────────────────────────────────────────
// ESCENARIO 5: ELIMINACIÓN DE UNA RESERVA
// ─────────────────────────────────────────────────────────

    @When("elimino la reserva")
    public void deleteReserva() {
        lastResponse = given().delete("/reservation/" + reservationId);
    }

    @Then("la reserva debe eliminarse correctamente")
    public void validarDelete() {
        assertThat(lastResponse.statusCode(), equalTo(200));
    }
}