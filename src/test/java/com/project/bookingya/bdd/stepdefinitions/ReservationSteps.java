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
import io.restassured.RestAssured;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
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
// GIVEN: CONFIGURACIÓN BASE — huésped y habitación
// ─────────────────────────────────────────────────────────

@Given("existe un huésped y una habitación disponible")
public void existeUnHuespedYHabitacion() {

    RestAssured.baseURI = BASE_URL;

    // Crear guest con datos únicos para evitar duplicados
    long unique = System.currentTimeMillis();

    lastResponse = given()
            .contentType("application/json")
            .body("{"
                    + "\"identification\": \"QA-" + unique + "\","
                    + "\"name\": \"QA User\","
                    + "\"email\": \"qa" + unique + "@bookingya.com\""
                    + "}")
            .post("/guest");

    guestId = lastResponse.jsonPath().getString("id");

    if (guestId == null) {
        throw new RuntimeException(
                "No se pudo crear el guest. Status: " + lastResponse.statusCode()
                        + " Body: " + lastResponse.asString()
        );
    }

    // Crear room con un código único
    lastResponse = given()
            .contentType("application/json")
            .body("{"
                    + "\"code\": \"HAB-" + unique + "\","
                    + "\"name\": \"Habitacion QA BDD\","
                    + "\"city\": \"Medellin\","
                    + "\"maxGuests\": 3,"
                    + "\"nightlyPrice\": 150000,"
                    + "\"available\": true"
                    + "}")
            .post("/room");

    roomId = lastResponse.jsonPath().getString("id");

    if (roomId == null) {
        throw new RuntimeException(
                "No se pudo crear la habitacion. Status: " + lastResponse.statusCode()
                        + " Body: " + lastResponse.asString()
        );
    }
}

// ─────────────────────────────────────────────────────────
// GIVEN: CONFIGURACIÓN CON RESERVA EXISTENTE
// ─────────────────────────────────────────────────────────

@Given("existe una reserva creada")
public void existeUnaReservaCreada() {

    // Primero se crea guest y room
    existeUnHuespedYHabitacion();

    // Luego se crea la reserva
    lastResponse = given()
            .contentType("application/json")
            .body("{"
                    + "\"guestId\": \"" + guestId + "\","
                    + "\"roomId\": \"" + roomId + "\","
                    + "\"checkIn\": \"2026-07-01T14:00:00\","
                    + "\"checkOut\": \"2026-07-05T12:00:00\","
                    + "\"guestsCount\": 2,"
                    + "\"notes\": \"Reserva BDD\""
                    + "}")
            .post("/reservation");

    reservationId = lastResponse.jsonPath().getString("id");

    if (reservationId == null) {
        throw new RuntimeException(
                "No se pudo crear la reserva. Status: " + lastResponse.statusCode()
                        + " Body: " + lastResponse.asString()
        );
    }
}

// ─────────────────────────────────────────────────────────
// ESCENARIO 1: CREACIÓN DE UNA RESERVA
// ─────────────────────────────────────────────────────────

@When("creo una reserva válida")
public void creoReserva() {

    lastResponse = given()
            .contentType("application/json")
            .body("{"
                    + "\"guestId\": \"" + guestId + "\","
                    + "\"roomId\": \"" + roomId + "\","
                    + "\"checkIn\": \"2026-07-01T14:00:00\","
                    + "\"checkOut\": \"2026-07-05T12:00:00\","
                    + "\"guestsCount\": 2,"
                    + "\"notes\": \"Reserva BDD\""
                    + "}")
            .post("/reservation");

    reservationId = lastResponse.jsonPath().getString("id");
}

@Then("la reserva se crea exitosamente")
public void validarCreacion() {
    assertThat("Status HTTP debe ser 200",
            lastResponse.statusCode(), equalTo(200));
    assertThat("La reserva debe tener un ID válido",
            lastResponse.jsonPath().getString("id"), notNullValue());
}

// ─────────────────────────────────────────────────────────
// ESCENARIO 2: OBTENCIÓN DE UNA RESERVA POR ID
// ─────────────────────────────────────────────────────────

@When("consulto la reserva por su ID")
public void consultoPorId() {
    lastResponse = given()
            .get("/reservation/" + reservationId);
}

@Then("debo obtener la reserva por ID correctamente")
public void validarGetById() {
    assertThat("Status HTTP debe ser 200",
            lastResponse.statusCode(), equalTo(200));
}

// ─────────────────────────────────────────────────────────
// ESCENARIO 3: CONSULTA DE RESERVAS POR HUÉSPED
// ─────────────────────────────────────────────────────────

@When("consulto las reservas del huésped")
public void consultoPorGuest() {
    lastResponse = given()
            .get("/reservation/guest/" + guestId);
}

@Then("debo obtener las reservas del huésped")
public void validarConsulta() {
    assertThat("Status HTTP debe ser 200",
            lastResponse.statusCode(), equalTo(200));
}

// ─────────────────────────────────────────────────────────
// ESCENARIO 4: ACTUALIZACIÓN DE UNA RESERVA EXISTENTE
// ─────────────────────────────────────────────────────────

@When("actualizo la reserva")
public void actualizarReserva() {

    lastResponse = given()
            .contentType("application/json")
            .body("{"
                    + "\"guestId\": \"" + guestId + "\","
                    + "\"roomId\": \"" + roomId + "\","
                    + "\"checkIn\": \"2026-08-10T14:00:00\","
                    + "\"checkOut\": \"2026-08-15T12:00:00\","
                    + "\"guestsCount\": 1,"
                    + "\"notes\": \"Reserva actualizada por BDD\""
                    + "}")
            .put("/reservation/" + reservationId);
}

@Then("la reserva debe actualizarse correctamente")
public void validarUpdate() {
    assertThat("Status HTTP debe ser 200",
            lastResponse.statusCode(), equalTo(200));
}

// ─────────────────────────────────────────────────────────
// ESCENARIO 5: ELIMINACIÓN DE UNA RESERVA
// ─────────────────────────────────────────────────────────

@When("elimino la reserva")
public void eliminarReserva() {
    lastResponse = given()
            .delete("/reservation/" + reservationId);
}

@Then("la reserva debe eliminarse correctamente")
public void validarDelete() {
    assertThat("Status HTTP debe ser 200",
            lastResponse.statusCode(), equalTo(200));
}
}
